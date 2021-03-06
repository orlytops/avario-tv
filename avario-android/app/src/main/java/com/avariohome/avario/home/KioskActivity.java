package com.avariohome.avario.home;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.avariohome.avario.service.AvarioReceiver;
import com.avariohome.avario.service.FloatingViewService;

/**
 * Created by orly on 9/22/17.
 */

public class KioskActivity extends Activity {

    private int permissionCheck;
    private PackageManager mPackageManager;

    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mAdminComponentName;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDevicePolicyManager = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);

        mAdminComponentName = AvarioReceiver.getComponentName(this);
        mPackageManager = this.getPackageManager();
        //call this to uninstall application
        //mDevicePolicyManager.clearDeviceOwnerApp("com.avariohome.avario");
        if (mDevicePolicyManager.isDeviceOwnerApp(
                getApplicationContext().getPackageName())) {
            Intent lockIntent = new Intent(getApplicationContext(),
                    BootActivity.class);

            mPackageManager.setComponentEnabledSetting(
                    new ComponentName(getApplicationContext(),
                            BootActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            startActivity(lockIntent);
            finish();
            stopService(new Intent(getApplicationContext(), FloatingViewService.class));

        } else {
            Intent intent = new Intent(KioskActivity.this,
                    BootActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
