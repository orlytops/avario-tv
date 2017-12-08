package com.avariohome.avario.apiretro.services;

import com.avariohome.avario.apiretro.models.Updates;

import retrofit2.http.GET;
import rx.Observable;

/**
 * Created by orly on 12/8/17.
 */

public interface VersionService {

    @GET("/binarybean/avarioHomePublic/master/versions.json")
    Observable<Updates> getVersion();
}
