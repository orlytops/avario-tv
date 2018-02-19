package com.tv.avario.apiretro.services;

import com.google.gson.JsonArray;

import retrofit2.http.GET;
import rx.Observable;

/**
 * Created by orly on 12/19/17.
 */

public interface StateService {

    @GET("/api/states")
    Observable<JsonArray> getApiStates();
}
