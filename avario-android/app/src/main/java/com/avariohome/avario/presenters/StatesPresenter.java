package com.avariohome.avario.presenters;

import com.avariohome.avario.apiretro.services.StateService;
import com.google.gson.JsonArray;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by orly on 12/19/17.
 */

public class StatesPresenter {

    private final StateService stateService;

    public StatesPresenter(StateService stateService) {
        this.stateService = stateService;
    }

    public void getUpdate(Observer<JsonArray> userObserver) {
        stateService.getApiStates()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userObserver);
    }
}
