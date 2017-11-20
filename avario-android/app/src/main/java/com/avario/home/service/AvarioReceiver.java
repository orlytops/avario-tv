package com.avario.home.service;

import android.content.ComponentName;
import android.content.Context;

/**
 * Handles events related to the managed profile.
 */
public class AvarioReceiver extends android.app.admin.DeviceAdminReceiver {
    private static final String TAG = "DeviceAdminReceiver";

    /**
     * @param context The context of the application.
     * @return The component name of this component in the given context.
     */
    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), AvarioReceiver.class);
    }
}