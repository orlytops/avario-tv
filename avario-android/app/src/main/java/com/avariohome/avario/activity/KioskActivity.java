package com.avariohome.avario.activity;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.avariohome.avario.R;
import com.avariohome.avario.service.DeviceAdminReceiver;

/**
 * Created by orly on 9/22/17.
 */

public class KioskActivity extends Activity {

    private int permissionCheck;
    private PackageManager mPackageManager;

    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mAdminComponentName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDevicePolicyManager = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);

        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
        mPackageManager = this.getPackageManager();

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
        } else {
            Toast.makeText(getApplicationContext(),
                    R.string.not_lock_whitelisted, Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
