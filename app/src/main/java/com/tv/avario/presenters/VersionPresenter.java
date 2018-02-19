package com.tv.avario.presenters;

import com.tv.avario.apiretro.models.Updates;
import com.tv.avario.apiretro.services.VersionService;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by orly on 12/8/17.
 */

public class VersionPresenter {

    private final VersionService versionService;

    public VersionPresenter(VersionService versionService) {
        this.versionService = versionService;
    }

    public void getVersion(Observer<Updates> versionObserver) {
        versionService.getVersion()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(versionObserver);
    }
}
