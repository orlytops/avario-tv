package com.tv.avario.bus;

import com.tv.avario.core.Notification;

/**
 * Created by orly on 12/8/17.
 */

public class ShowNotification {
    private Notification notification;

    public ShowNotification(Notification notification){
        this.notification = notification;
    }

    public Notification getNotification() {
        return notification;
    }
}
