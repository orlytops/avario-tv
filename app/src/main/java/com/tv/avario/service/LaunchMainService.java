package com.tv.avario.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class LaunchMainService extends Service {

  private static final long   INTERVAL        = TimeUnit.SECONDS.toMillis(2);
  // periodic interval to check in seconds -> 2 seconds
  private static final String TAG             = LaunchMainService.class.getSimpleName();
  private static final String PREF_KIOSK_MODE = "pref_kiosk_mode";

  private Thread  t       = null;
  private Context ctx     = null;
  private boolean running = false;

  @Override
  public void onDestroy() {
    Log.i(TAG, "Stopping service 'KioskService'");
    running = false;
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    final Intent intentd = new Intent();
    intentd.setComponent(
        new ComponentName("com.tv.avario", "com.tv.avario.home.MainDialogActivity"));
    intentd.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intentd);
    stopSelf();
    return Service.START_NOT_STICKY;
  }


  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}