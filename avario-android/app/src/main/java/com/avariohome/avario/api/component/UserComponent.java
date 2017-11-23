package com.avariohome.avario.api.component;


import com.avariohome.avario.apiretro.ApiModule;
import com.avariohome.avario.fragment.SettingsDialogFragment;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by orly on 9/6/17.
 */
@Singleton
@Component(modules = {ApiModule.class})
public interface UserComponent {

    void inject(SettingsDialogFragment settingsDialogFragment);

}
