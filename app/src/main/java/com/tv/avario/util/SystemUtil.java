package com.tv.avario.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.tv.avario.home.BootActivity;

public class SystemUtil {

    /**
     * Force reboot app to be certain that any changes made in the
     * bootstrap is applied.
     *
     * @param context Application context.
     */
    public static void rebootApp(Context context) {
        Intent mStartActivity = new Intent(context, BootActivity.class);
        int mPendingIntentId = 22443; // No other pending intent should contain this id.
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }
}
