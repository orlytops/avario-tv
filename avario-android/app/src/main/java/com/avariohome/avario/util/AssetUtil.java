package com.avariohome.avario.util;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.*;
import android.view.View;
import android.widget.ImageView;

import com.avariohome.avario.core.Config;
import com.avariohome.avario.exception.AvarioException;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;


/**
 * Created by aeroheart-c6 on 06/01/2017.
 */
public class AssetUtil {
    public static final String TAG = "Avario/AssetUtil";

    public static Drawable getPlaceholderDrawable(Context context) {
        Drawable drawable;

        if (PlatformUtil.isLollipopOrNewer())
            drawable = context
                    .getResources()
                    .getDrawable(
                            com.avariohome.avario.R.drawable.ic__placeholder,
                            context.getTheme()
                    );
        else
            drawable = context
                    .getResources()
                    .getDrawable(com.avariohome.avario.R.drawable.ic__placeholder);

        return drawable;
    }

    public static String getAssetRoot(Context context) {
        Config conf = Config.getInstance(context);
        String url;

        url = String.format("%s%s", conf.getHttpDomain(), conf.getAssetRoot());
        url = String.format(url, context.getResources().getDisplayMetrics().density);

        return url;
    }

    public static DrawableLoader toDrawable(Context context, int assetId, DrawableLoader.Callback callback) {
        // TODO: 11/6/17 This is were multiple asset download gets called John notes.
        return AssetUtil.toDrawable(
                context,
                AssetUtil.toAbsoluteURLs(context, context.getResources().getStringArray(assetId)),
                callback
        );
    }

    public static DrawableLoader toDrawable(Context context, String[] urls, DrawableLoader.Callback callback) {
        DrawableLoader task;

        task = new DrawableLoader(context, callback);
        task.execute(Arrays.asList(urls));

        return task;
    }

    public static Drawable toDrawable(Context context, int assetId) {
        return AssetUtil.toDrawable(
                context,
                AssetUtil.toAbsoluteURLs(context, context.getResources().getStringArray(assetId))
        );
    }

    public static Drawable toDrawable(Context context, String[] urls) {
        DrawableLoader task;

        task = new DrawableLoader(context, null);
        task.execute(Arrays.asList(urls));

        try {
            return task.getDrawable(task.get());
        } catch (CancellationException | ExecutionException | InterruptedException exception) {
            return task.getDrawable(null);
        }
    }

    public static String[] toAbsoluteURLs(Context context, String[] urls) {
        String[] output = new String[urls.length];
        String root = AssetUtil.getAssetRoot(context);

        for (int index = 0; index < urls.length; index++)
            output[index] = String.format("%s%s", root, urls[index]);

        /*for (int index = 0; index < urls.length; index++)
            Log.d("Output", output[index]);*/
        return output;
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Callbacks
     ***********************************************************************************************
     */
    public static class ImageViewCallback implements DrawableLoader.Callback {
        public ImageView view;

        public ImageViewCallback(ImageView view) {
            this.view = view;
        }

        @Override
        public void onSuccess(Drawable drawable) {
            this.view.setImageDrawable(drawable);
        }

        @Override
        public void onFailure(AvarioException exception) {
            this.view.setImageDrawable(AssetUtil.getPlaceholderDrawable(this.view.getContext()));
        }

        @Override
        public void onCancel() {
        }
    }

    public static class BackgroundCallback implements DrawableLoader.Callback {
        public View view;

        public BackgroundCallback(View view) {
            this.view = view;
        }

        @Override
        public void onSuccess(Drawable drawable) {
            this.view.setBackground(drawable);
        }

        @Override
        public void onFailure(AvarioException exception) {
        }

        @Override
        public void onCancel() {
        }
    }
}
