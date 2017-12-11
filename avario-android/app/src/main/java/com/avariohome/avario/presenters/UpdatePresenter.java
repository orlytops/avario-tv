package com.avariohome.avario.presenters;

import com.avariohome.avario.apiretro.models.Version;
import com.avariohome.avario.apiretro.services.UpdateService;

import okhttp3.ResponseBody;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by orly on 11/23/17.
 */

public class UpdatePresenter {

    private final UpdateService updateService;

    public UpdatePresenter(UpdateService updateService) {
        this.updateService = updateService;
    }

    public void getUpdate(Observer<ResponseBody> userObserver) {
        updateService.getUpdate()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userObserver);
    }

    public void getVersion(Observer<Version> userObserver) {
        updateService.getVersion()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userObserver);
    }
}
