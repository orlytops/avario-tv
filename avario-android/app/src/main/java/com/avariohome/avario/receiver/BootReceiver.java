package com.avariohome.avario.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.avariohome.avario.activity.MainActivity;

/**
 * Created by orly on 9/22/17.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent myIntent = new Intent(context, MainActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myIntent);
    }
}
