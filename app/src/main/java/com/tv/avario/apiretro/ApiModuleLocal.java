package com.tv.avario.apiretro;

import com.avario.core.Config;
import com.tv.avario.apiretro.models.ProgressResponseBody;
import com.tv.avario.apiretro.services.VersionService;
import com.tv.avario.util.Log;

import org.eclipse.paho.client.mqttv3.internal.websocket.Base64;

import java.io.IOException;

import javax.inject.Singleton;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import dagger.Module;
import dagger.Provides;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by orly on 9/6/17.
 */

@Module
public class ApiModuleLocal {

    private Config config;

    public ApiModuleLocal() {
    }

    @Provides
    @Singleton
    Retrofit providesRetrofit() {

        config = Config.getInstance();

        String domain = "https://raw.githubusercontent.com/";
        /*if (config.getHttpHost() != null) {
            Log.d("Domain", config.getHttpDomain());
            domain = config.getHttpDomain();
        }*/

        //use BuildConfig.BASEURL for freelancer API
        //use BuildConfig.MOCKURL for freelancer API
        return new Retrofit.Builder()
                .baseUrl(domain)
                .client(getClient())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
    }

    private OkHttpClient getClient() {
        config = Config.getInstance();
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("Content-Type", "application/json")
                        .header("Authorization", String.format("Basic %s", Base64.encode(String.format(
                                "%s:%s",
                                config.getUsername(),
                                config.getPassword()
                        ))))
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }
        });
        client.interceptors().add(
                new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));

        client.addNetworkInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                Log.d("Headers", originalResponse.headers().toString());
                return originalResponse.newBuilder()
                        .body(new ProgressResponseBody(originalResponse.body(), null))
                        .build();
            }
        });
        client.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        return client.build();
    }

    @Provides
    @Singleton
    public VersionService providesUserService(Retrofit retrofit) {
        return retrofit.create(VersionService.class);
    }

}
