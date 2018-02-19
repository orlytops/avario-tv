package com.tv.avario.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.avario.core.Config;
import com.tv.avario.api.component.DaggerUserComponent;
import com.tv.avario.api.component.UserComponent;
import com.tv.avario.apiretro.models.Version;
import com.tv.avario.apiretro.services.UpdateService;
import com.tv.avario.apiretro.services.VersionService;
import com.tv.avario.bus.TriggerUpdate;
import com.tv.avario.presenters.UpdatePresenter;
import com.tv.avario.presenters.VersionPresenter;
import com.tv.avario.util.Log;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

import rx.Observer;

/**
 * Created by orly on 12/5/17.
 */

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    @Inject
    UpdateService userService;

    @Inject
    VersionService versionService;

    private UserComponent userComponent;

    public AlarmReceiver() {
        userComponent = DaggerUserComponent.builder().build();
        userComponent.inject(this);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "Received Alarm");
        final Config config = Config.getInstance();

        UpdatePresenter updatePresenter = new UpdatePresenter(userService);
        final VersionPresenter versionPresenter = new VersionPresenter(versionService);
        updatePresenter.getVersion(new Observer<Version>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Version version) {
                Log.d("Version", version.getVersion());
                try {
                    PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                    String versionName = pInfo.versionName;
                    Log.d("Version code", versionName + "");
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                if (needsUpdate(context, version.getVersion()) && !version.getVersion().equals(config.getToIgnore())) {
                    EventBus.getDefault().post(new TriggerUpdate(version.getVersion()));
                }
            }
        });
    }


    /**
     * Checks if the version passes is a higher version than the current version installed
     *
     * @param context
     * @param version the available version tha got from local/tablet/version.json
     * @return true if the app needs an update
     * false if the app is up to date
     */
    private boolean needsUpdate(Context context, String version) {
        try {

            //get current Version Code
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            String currentVersionName = info.versionName;

            if (!version.equals(currentVersionName)) {
                //version string not the same, version is NOT up to date

                Boolean updateNeeded = false;
                String[] currentVersionCodeArray = currentVersionName.split("\\.");
                String[] storeVersionCodeArray = version.split("\\.");

                int maxLength = currentVersionCodeArray.length;
                if (storeVersionCodeArray.length > maxLength) {
                    maxLength = storeVersionCodeArray.length;
                }

                for (int i = 0; i < maxLength; i++) {

                    try {
                        if (Integer.parseInt(storeVersionCodeArray[i]) > Integer.parseInt(currentVersionCodeArray[i])) {
                            updateNeeded = true;
                            continue;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        //store version code length > current version length = version needs to be updated
                        //if store version length is shorter, the if-statement already did the job
                        if (storeVersionCodeArray.length > currentVersionCodeArray.length) {
                            updateNeeded = true;
                        }
                    }
                }

                if (updateNeeded) {
                    return true;
                }

            } else {
                return false;
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }
}
