package com.tv.avario.apiretro;

import com.avario.core.Config;
import com.tv.avario.api.APIClient;
import com.tv.avario.apiretro.models.ProgressResponseBody;
import com.tv.avario.apiretro.services.StateService;
import com.tv.avario.apiretro.services.UpdateService;
import com.tv.avario.apiretro.services.VersionService;
import com.tv.avario.core.StateArray;
import com.tv.avario.exception.AvarioException;
import com.tv.avario.util.Connectivity;
import com.tv.avario.util.Log;

import org.eclipse.paho.client.mqttv3.internal.websocket.Base64;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
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
public class ApiModule {

  private Config     config;
  private StateArray stateArray;

  private String username = "";
  private String password = "";

  public ApiModule() {
  }

  @Provides
  @Singleton
  Retrofit providesRetrofit() {

    stateArray = StateArray.getInstance();
    config = Config.getInstance();

    String domain = "https://192.168.0.18:22443/";
    Log.d("Domain", config.getHttpDomain());


    try {
      domain = stateArray.getHTTPHost("ip1");
    } catch (AvarioException e) {
      if (config.getHttpHost() != null) {
        domain = config.getHttpDomain();
      }
      e.printStackTrace();
    }

    if (domain == null) {
      domain = "https://192.168.0.9:23443/";
    }

    Log.d("Domain/API/module", domain + " " + Connectivity.isConnectedToLan());
    //use BuildConfig.BASEURL for freelancer API
    //use BuildConfig.MOCKURL for freelancer API
    return new Retrofit.Builder()
        .baseUrl(domain)
        .client(getClient())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();
  }

  @Provides
  @Singleton
  @Named("RetrofitGithub")
  Retrofit providesRetrofitGithub() {

    config = Config.getInstance();

    String domain = "https://raw.githubusercontent.com/";
        /*if (config.getHttpHost() != null) {
            Log.d("Domain", config.getHttpDomain());
            domain = config.getHttpDomain();
        }*/

    return new Retrofit.Builder()
        .baseUrl(domain)
        .client(getClientRetrofit())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();
  }


  /**
   * @return the OkHttpclient here you set all the interceptors for the client
   */
  private OkHttpClient getClient() {
    stateArray = StateArray.getInstance();
    config = Config.getInstance();
    try {
      username = stateArray.getHTTPUsername("ip1");
      password = stateArray.getHTTPPassword("ip1");
    } catch (AvarioException e) {
      if (config.getHttpHost() != null) {
        username = config.getUsername();
        password = config.getPassword();
      }
      e.printStackTrace();
    }

    if (username == null) {
      username = "avario";
      password = "avario";
    }

    Log.d("Username", username);
    Log.d("Password", password);
    OkHttpClient.Builder client = new OkHttpClient.Builder();
    client.addInterceptor(new Interceptor() {
      @Override
      public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        Request request = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("Authorization", String.format("Basic %s", Base64.encode(String.format(
                "%s:%s",
                username,
                password
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


    client.sslSocketFactory(APIClient.getSSLContext().getSocketFactory());

    client.connectTimeout(100, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS);
    return client.build();
  }

  private OkHttpClient getClientRetrofit() {
    config = Config.getInstance();
    OkHttpClient.Builder client = new OkHttpClient.Builder();
        /*client.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("Content-Type", "application/json")
                        .header("Authorization", String.format("Basic %s", Base64.encode(String
                        .format(
                                "%s:%s",
                                config.getUsername(),
                                config.getPassword()
                        ))))
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }
        });*/
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

    client.sslSocketFactory(APIClient.getSSLContext().getSocketFactory());

    return client.build();
  }

  @Provides
  @Singleton
  public UpdateService providesUserService(Retrofit retrofit) {
    return retrofit.create(UpdateService.class);
  }

  @Provides
  @Singleton
  public StateService providesStateService(Retrofit retrofit) {
    return retrofit.create(StateService.class);
  }

  @Provides
  @Singleton
  public VersionService providesVersionService(Retrofit retrofit) {
    return retrofit.create(VersionService.class);
  }

}
