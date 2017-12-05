package com.avariohome.avario.apiretro.services;


import com.avariohome.avario.apiretro.models.Version;

import okhttp3.ResponseBody;
import retrofit2.http.GET;
import rx.Observable;

/**
 * Created by orly on 9/6/17.
 */

public interface UpdateService {

    @GET("/local/tablet/app-release.apk")
    Observable<ResponseBody> getUpdate();

    @GET("local/tablet/version.json")
    Observable<Version> getVersion();

}
