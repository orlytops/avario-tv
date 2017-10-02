package com.avariohome.avario.activity;

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
import android.widget.Toast;

import com.avariohome.avario.R;
import com.avariohome.avario.service.DeviceAdminReceiver;
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

        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
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

            //mDevicePolicyManager.clearDeviceOwnerApp("com.avariohome.avario");
        } else {
            Intent intent = new Intent(getApplicationContext(),
                    BootActivity.class);
            startActivity(intent);
            Toast.makeText(getApplicationContext(),
                    R.string.not_lock_whitelisted, Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
