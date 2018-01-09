package com.avariohome.avario.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.avariohome.avario.R;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.util.AssetUtil;
import com.avariohome.avario.util.Connectivity;
import com.avariohome.avario.util.DrawableLoader;
import com.avariohome.avario.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by orly on 12/20/17.
 */

public class DownloadImageService extends Service {
    private static final String TAG = "Avario/DownloadImageService";

    private BatchAssetLoaderTask task;
    private boolean isRunning = false;
    int count = 0;

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final Config config = Config.getInstance();
        count++;
        Log.d(TAG, "Service started: " + count);

        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                if (config.isSet() && !isRunning) {
                    isRunning = true;
                    handleDownloadImage();
                }
                subscriber.onNext(new Object());
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {
                    }
                });
        return super.onStartCommand(intent, flags, startId);

    }

    private void handleDownloadImage() {
        //deleteAssetCache(getCacheDir());
        //AssetLoaderTask.setPicasso(null);
        loadAssets();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean deleteAssetCache(File dir) {
        Log.i(TAG, "deleting asset cache...");

        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();

            Log.i(TAG, "Length " + children.length);

            for (String child : children)
                deleteAssetCache(new File(dir, child));

            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }

    private void loadAssets() {
        Context context = getApplicationContext();
        Resources res = context.getResources();
        String pkg = context.getPackageName();

        Pattern pattern = Pattern.compile("^(ic|bg)__.+");
        Field[] fields = R.array.class.getDeclaredFields();

        List<String> urls = new ArrayList<>();

        for (Field field : fields) {
            if (!pattern.matcher(field.getName()).matches())
                continue;
            String[] paths;

            paths = res.getStringArray(res.getIdentifier(field.getName(), "array", pkg));
            paths = AssetUtil.toAbsoluteURLs(context, paths);

            urls.addAll(Arrays.asList(paths));
        }

        task = new BatchAssetLoaderTask(context.getApplicationContext());
        task.execute(urls);
    }

    public class BatchAssetLoaderTask extends DrawableLoader {
        BatchAssetLoaderTask(Context context) {
            super(context, null);
        }


        @Override
        protected void onCancelled(Map<int[], Bitmap> assets) {

        }

        @Override
        protected void onPostExecute(Map<int[], Bitmap> assets) {
            Config config = Config.getInstance();
            config.setIsImageDownloaded(true);

            if (Connectivity.isConnectedToLan()) {
                config.setIsImageLan(true);
            } else {
                config.setIsImageLan(false);
            }
            stopSelf();
            Log.d(TAG, "-----------------------------Asset download finished!-----------------------------");
        }
    }

    @Override
    public void onDestroy() {

        isRunning = false;

        Log.i(TAG, "Service onDestroy");
    }
}
