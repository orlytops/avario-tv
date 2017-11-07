package com.avariohome.avario.util;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.exception.AvarioException;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HostnameVerifier;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;


/**
 * Created by aeroheart-c6 on 06/01/2017.
 */
public class AssetUtil {
    public static final String TAG = "Avario/AssetUtil";
    private static Picasso picasso;

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

    /*public static DrawableLoader toDrawable(Context context, int assetId, DrawableLoader.Callback callback, View image) {


        String[] urls = AssetUtil.toAbsoluteURLs(context, context.getResources().getStringArray(assetId));
        for (String url : urls) {
            if (image instanceof ImageButton) {
                ImageButton imageView = (ImageButton) image;
                AssetUtil.picasso(context).load(url).into(imageView);
            } else if (image instanceof ImageView) {
                ImageView imageViw = (ImageView) image;
                AssetUtil.picasso(context).load(url).into(imageViw);
            }
        }

        return AssetUtil.toDrawable(
                context,
                AssetUtil.toAbsoluteURLs(context, context.getResources().getStringArray(assetId)),
                callback
        );
    }*/

    public static Picasso picasso(Context context) {
        if (picasso == null) {
            HostnameVerifier verifier = APIClient.getDevHostnameVerifier();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            OkHttpClient client;

            builder = builder
                    .cache(OkHttp3Downloader.createDefaultCache(context))
                    .addNetworkInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
                            Config config = Config.getInstance();
                            String credential = Credentials.basic(
                                    config.getUsername(),
                                    config.getPassword()
                            );

                            return chain.proceed(
                                    chain.request().newBuilder()
                                            .addHeader("Authorization", credential)
                                            .build()
                            );
                        }
                    });

            if (verifier != null)
                builder.hostnameVerifier(verifier);

            client = builder.build();

            picasso = new Picasso.Builder(context)
                    .listener(picassoListener)
                    .downloader(new OkHttp3Downloader(client))
                    .build();
            //Picasso.setSingletonInstance(AssetUtil.picasso);
            //picasso.setIndicatorsEnabled(true);
        }
        return picasso;
    }

    public static Picasso.Listener picassoListener = new Picasso.Listener() {
        @Override
        public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
            exception.printStackTrace();
        }
    };

    public static void loadImage(Context context, int assetId, DrawableLoader.Callback callback, View image) {

        String[] urls = AssetUtil.toAbsoluteURLs(context, context.getResources().getStringArray(assetId));
        for (String url : urls) {
            if (image instanceof ImageButton) {
                ImageButton imageView = (ImageButton) image;
                AssetUtil.picasso(context).load(url).into(imageView);
            } else if (image instanceof ImageView) {
                ImageView imageViw = (ImageView) image;
                AssetUtil.picasso(context).load(url).into(imageViw);
            }
        }
    }

    public static void loadImage(Context context, String[] urls, DrawableLoader.Callback callback, View image) {
        for (String url : urls) {
            if (image instanceof ImageButton) {
                ImageButton imageView = (ImageButton) image;
                AssetUtil.picasso(context).load(url).into(imageView);
            } else if (image instanceof ImageView) {
                ImageView imageViw = (ImageView) image;
                AssetUtil.picasso(context).load(url).into(imageViw);
            }
        }
    }

    public static DrawableLoader toDrawable(Context context, int assetId, DrawableLoader.Callback callback) {

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
