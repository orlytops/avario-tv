package com.avariohome.avario.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.avariohome.avario.home.BootActivity;

/**
 * Created by orly on 11/23/17.
 */

public class InstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
            Intent serviceIntent = new Intent();
            serviceIntent.setClass(context, BootActivity.class);
            serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(serviceIntent);
        } else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
            Intent serviceIntent = new Intent();
            serviceIntent.setClass(context, BootActivity.class);
            serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(serviceIntent);
        }
    }
}
