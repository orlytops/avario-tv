package com.avariohome.avario.core;


import com.avariohome.avario.Application;
import com.avariohome.avario.util.Log;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by aeroheart-c6 on 21/03/2017.
 */
public class APITimers {
    private static final String TAG = "Avario/APITimers";

    private static Map<String, Runnable> runnables = new HashMap<>();
    private static Map<String, Boolean> locks = new HashMap<>();

    public static void lock(String lockId) {
        APITimers.locks.put(lockId, true);
    }

    public static void unlock(String lockId) {
        APITimers.locks.remove(lockId);
    }

    public static boolean isLocked(String lockId) {
        return APITimers.locks.containsKey(lockId);
    }

    public static void reset(String timerId, String[] entityIds) {
        ExpireRunnable runnable = APITimers.getRunnable(timerId, entityIds);

        APITimers.invalidate(timerId);

        Application.workHandler.postDelayed(
            runnable,
            Config.getInstance().getAPIErrorDelay()
        );

        APITimers.runnables.put(timerId, runnable);
    }

    public static void invalidate(String timerId) {
        if (!APITimers.runnables.containsKey(timerId))
            return;

        Application.workHandler.removeCallbacks(APITimers.runnables.get(timerId));
        APITimers.forget(timerId);
    }

    public static ExpireRunnable getRunnable(String timerId, String[] entityIds) {
        return new ExpireRunnable(timerId, entityIds);
    }

    private static void forget(String timerId) {
        if (timerId == null || !APITimers.runnables.containsKey(timerId))
            return;

        APITimers.runnables.remove(timerId);
    }


    /*
     ***********************************************************************************************
     * Inner Classes
     ***********************************************************************************************
     */
    private static class ExpireRunnable implements Runnable {
        private String timerId;
        private String[] entityIds;

        public ExpireRunnable(String timerId, String[] entityIds) {
            this.timerId = timerId;
            this.entityIds = entityIds;
        }

        @Override
        public void run() {
            StateArray states = StateArray.getInstance();
            StringBuilder builder;

            // logging
            builder = new StringBuilder();
            builder
                .append("API timer expired with id: ")
                .append(this.timerId)
                .append("\n");

            if (this.entityIds == null)
                builder.append("null");
            else {
                builder.append("For entities:\n");

                for (String entityId : this.entityIds)
                    builder
                        .append(entityId)
                        .append("\n");
            }

            Log.i(TAG, builder.toString());
            // end logging

            if (this.entityIds == null || this.entityIds.length == 0)
                states.broadcastChanges();
            else
                for (String entityId : this.entityIds)
                    states.broadcastChanges(entityId, StateArray.FROM_TIMER);

            APITimers.forget(this.timerId);
        }
    }
}
