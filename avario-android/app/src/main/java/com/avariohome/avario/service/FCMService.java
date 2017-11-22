package com.avariohome.avario.service;


import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.avariohome.avario.Constants;
import com.avariohome.avario.core.Notification;
import com.avariohome.avario.core.NotificationArray;
import com.avariohome.avario.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


/**
 * Created by aeroheart-c6 on 07/07/2017.
 */
public class FCMService extends FirebaseMessagingService {
    private static final String TAG = "Avario/FCMService";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        NotificationArray array = NotificationArray.getInstance();
        Notification notification = Notification.fromRemoteMessage(message);

        if (notification.data.length() == 0)
            return; // no need to do anything

        array.addNotification(notification);
        array.deleteExpired();

        this.broadcastNotif(notification);
        showMessageInformation(message);
    }

    private void showMessageInformation(RemoteMessage message) {
        StringBuilder builder = new StringBuilder();

        builder
            .append("To: ")
            .append(message.getTo())
            .append("\nFrom: ")
            .append(message.getFrom())
            .append("\nType: ")
            .append(message.getMessageType())
            .append("\nMessage Id: ")
            .append(message.getMessageId())
            .append("\nMessage Date: ")
            .append(message.getSentTime())
            .append("\nCollapse Key: ")
            .append(message.getCollapseKey())
            .append("\nTime to Live: ")
            .append(message.getTtl());

        if (message.getNotification() != null)
            builder
                .append("\nNotification:")
                .append("\n\tTitle: ")
                .append(message.getNotification().getTitle())
                .append("\n\tBody: ")
                .append(message.getNotification().getBody());

        builder
            .append("\nPayload:\n")
            .append(message.getData().size() > 0 ? message.getData() : "--");

        Log.d(TAG, builder.toString());

    }

    private void broadcastNotif(Notification notification) {
        Intent intent = new Intent()
            .setAction(Constants.BROADCAST_NOTIF)
            .putExtra("notification", notification);

        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent);
    }
}
