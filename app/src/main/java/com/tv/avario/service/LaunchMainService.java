package com.tv.avario.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class LaunchMainService extends Service {

  private static final String TAG = LaunchMainService.class.getSimpleName();

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