package com.tv.avario.api.component;


import com.tv.avario.apiretro.ApiModule;
import com.tv.avario.fragment.NotificationDialogFragment;
import com.tv.avario.fragment.SettingsDialogFragment;
import com.tv.avario.receiver.AlarmReceiver;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by orly on 9/6/17.
 */
@Singleton
@Component(modules = {ApiModule.class})
public interface UserComponent {

  void inject(SettingsDialogFragment settingsDialogFragment);

  void inject(AlarmReceiver alarmReceiver);

  void inject(NotificationDialogFragment notificationDialogFragment);

}
