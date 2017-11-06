package com.avariohome.avario.util;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.avariohome.avario.Constants;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.exception.AvarioException;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;


/**
 * Created by aeroheart-c6 on 06/01/2017.
 */
public abstract class AssetLoaderTask<Result> extends AsyncTask<List<String>, Void, Result> {
    public static final String TAG = "Avario/AssetLoader";

    private static Picasso picasso;

    public static Picasso picasso(Context context) {
        if (AssetLoaderTask.picasso == null) {
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
//                .authenticator(new Authenticator() {
//                    @Override
//                    public Request authenticate(Route route, Response response) throws IOException {
//                        Config config = Config.getInstance();
//                        String credential = Credentials.basic(
//                            config.getUsername(),
//                            config.getPassword()
//                        );
//
//                        return response.request().newBuilder()
//                            .header("Authorization", credential)
//                            .build();
//                    }
//                })

            if (verifier != null)
                builder.hostnameVerifier(verifier);

            client = builder.build();

            AssetLoaderTask.picasso = new Picasso.Builder(context)
                .downloader(new OkHttp3Downloader(client))
                .build();

            Picasso.setSingletonInstance(AssetLoaderTask.picasso);
        }

        return AssetLoaderTask.picasso;
    }

    protected Context context;

    protected AvarioException exception;

    public AssetLoaderTask(Context context) {
        this.context = context;
        this.exception = null;
    }

    /**
     * Performs actual fetching of the images. Note however that what gets posted in the results
     * will only be the first set of bitmaps. Passing in multiple params will facilitate in "warming
     * up" the cache.
     *
     * @param params
     * @return
     */
    @Override
    protected Result doInBackground(List<String> ... params) {
        this.exception = null;

        try {
            return this.processFetch(this.fetch(params[0]));
        }
        catch (AvarioException exception) {
            this.exception = exception;
            return null;
        }
    }

    @Override
    protected void onCancelled(Result result) {
        Log.d(TAG, "Task has been cancelled");
    }

    /**
     * Fetches images. In the case of an empty URL, it will add a null in the items. IF the passed
     * list of urls is null, then the method returns null.
     *
     * @param urls
     * @return
     * @throws AvarioException
     */
    protected List<Bitmap> fetch(List<String> urls) throws AvarioException {
        List<Bitmap> bitmaps = new ArrayList<>();

        if (urls == null)
            return null;

        for (String url : urls) {
            Bitmap bitmap;

            try {
                android.util.Log.v(TAG, "Downloading " + url);
                bitmap = AssetLoaderTask
                    .picasso(this.context.getApplicationContext())
                    .load(url)
                    .get();

                bitmaps.add(bitmap);
            }
            catch (IllegalArgumentException exception) {
                bitmaps.add(null);
            }
            catch (IOException exception) {
                throw new AvarioException(
                    Constants.ERROR_ASSET_UNREACHABLE,
                    exception,
                    new Object[] { url }
                );
            }

            if (this.isCancelled())
                break;
        }

        return bitmaps;
    }

    protected abstract Result processFetch(List<Bitmap> bitmaps);
}
