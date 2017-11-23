package com.avariohome.avario.apiretro.services;


import okhttp3.ResponseBody;
import retrofit2.http.GET;
import rx.Observable;

/**
 * Created by orly on 9/6/17.
 */

public interface UpdateService {

    @GET("/local/updates/update.apk")
    Observable<ResponseBody> getUpdate();

}
