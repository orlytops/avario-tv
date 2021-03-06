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
import com.avariohome.avario.core.StateArray;
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

        StateArray stateArray = StateArray.getInstance();
        if (!stateArray.hasData()) {
            try {
                stateArray.load();
            } catch (AvarioException e) {
                e.printStackTrace();
            }
        }

        //Identifying whether which host should be use either WAN/LAN HTTP host.
        String host;
        try {
            if (conf.isImageDownloaded()) {
                host = stateArray.getHTTPHost("ip1", conf.isImageLan());
            } else {
                host = stateArray.getHTTPHost("ip1");
            }

        } catch (AvarioException e) {
            host = conf.getHttpHost();
            e.printStackTrace();
        }

        String url;

        url = String.format("%s%s", host, conf.getAssetRoot());
        url = String.format(url, context.getResources().getDisplayMetrics().density);

        return url;
    }

    //Used during settings asset download
    public static String getAssetRoot(Context context, boolean isLan) {
        Config conf = Config.getInstance(context);

        StateArray stateArray = StateArray.getInstance();
        if (!stateArray.hasData()) {
            try {
                stateArray.load();
            } catch (AvarioException e) {
                e.printStackTrace();
            }
        }

        String host;
        try {
            if (conf.isImageDownloaded()) {
                host = stateArray.getHTTPHost("ip1", isLan);
            } else {
                host = stateArray.getHTTPHost("ip1");
            }

        } catch (AvarioException e) {
            host = conf.getHttpHost();
            e.printStackTrace();
        }

        String url;

        url = String.format("%s%s", host, conf.getAssetRoot());
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

    /**
     * @param context
     * @return current picasso instance used in the app
     */
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

    /**
     * url image directly loaded by picasso
     * picasso handles the download if the image is already downloaded
     *
     * @param context
     * @param assetId  id for the current image
     * @param callback listener for the image on load
     * @param image    imageview where the url is being loaded
     */
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

    /**
     * urls image directly loaded by picasso
     * picasso handles the download if the image is already downloaded
     *
     * @param context
     * @param urls     that's need to be loaded
     * @param callback callback listener for image download
     * @param image    imageview where the url is being loaded
     */
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

    /**
     * @param context
     * @param assetId  id for the asset that should be used
     * @param callback return callback for a successful image load
     * @return drawable that is being converted by picasso from the downloaded url
     */
    public static DrawableLoader toDrawable(Context context, int assetId, DrawableLoader.Callback callback) {

        return AssetUtil.toDrawable(
                context.getApplicationContext(),
                AssetUtil.toAbsoluteURLs(context, context.getResources().getStringArray(assetId)),
                callback
        );
    }

    /**
     * @param context
     * @param urls     that should be handled by picasso
     * @param callback return callback for a successful image load
     * @return drawable that is being converted by picasso from the downloaded urs
     */
    public static DrawableLoader toDrawable(Context context, String[] urls, DrawableLoader.Callback callback) {
        DrawableLoader task;

        task = new DrawableLoader(context.getApplicationContext(), callback);
        task.execute(Arrays.asList(urls));

        return task;
    }

    public static Drawable toDrawable(Context context, int assetId) {
        return AssetUtil.toDrawable(
                context,
                AssetUtil.toAbsoluteURLs(context, context.getResources().getStringArray(assetId))
        );
    }


    /**
     * @param context
     * @param urls    that should be handled
     * @return drawable that gets from urls
     */
    public static Drawable toDrawable(Context context, String[] urls) {
        DrawableLoader task;

        task = new DrawableLoader(context.getApplicationContext(), null);
        task.execute(Arrays.asList(urls));

        try {
            return task.getDrawable(task.get());
        } catch (CancellationException | ExecutionException | InterruptedException exception) {
            return task.getDrawable(null);
        }
    }


    /**
     * @param context
     * @param urls    that should be handled
     * @return string urls
     */
    public static String[] toAbsoluteURLs(Context context, String[] urls) {
        String[] output = new String[urls.length];
        String root = AssetUtil.getAssetRoot(context.getApplicationContext());

        for (int index = 0; index < urls.length; index++)
            output[index] = String.format("%s%s", root, urls[index]);

        /*for (int index = 0; index < urls.length; index++)
            Log.d("Output", output[index]);*/
        return output;
    }

    /**
     * @param context
     * @param urls    that should be handled
     * @return string urls
     */
    public static String[] toAbsoluteURLs(Context context, String[] urls, boolean isLan) {
        String[] output = new String[urls.length];
        String root = AssetUtil.getAssetRoot(context.getApplicationContext(), isLan);

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
            //this.view.setImageDrawable(AssetUtil.getPlaceholderDrawable(this.view.getContext()));
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
