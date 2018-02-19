package com.tv.avario.core;


import android.os.Parcel;
import android.os.Parcelable;

import com.tv.avario.util.Log;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Represents a single notification object to be saved into the file and loaded back in. This will
 * be the object the app will primarily interact now
 * <p>
 * Created by aeroheart-c6 on 7/11/17.
 */
public class Notification implements Parcelable {
    private static final String TAG = "Avario/Notification";
    private static Creator<Notification> CREATOR = new Parcelable.Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel source) {
            Notification instance = new Notification();

            try {
                instance.data = new JSONObject(source.readString());
            } catch (JSONException exception) {
                instance.data = new JSONObject();
            }

            return instance;
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };

    /**
     * data - the entire JSON data representing the Notification. This contains the following
     * properties:
     * * message_id - [String] message id from firebase
     * * title - [String] title from the notifications payload
     * * body - [String] content from the notifications payload
     * * date_sent - [long] the time in milliseconds that the message was sent
     * * ttl - [long] time in milliseconds to expend before it expires.
     * APPARENTLY, RemoteMessage.getTtl() always returns 0 and is unreliable.
     * * is_read - [boolean] in-app flag to determine if user has read this notification or not
     */
    public JSONObject data;

    public Notification() {
        this(new JSONObject());
    }

    public Notification(JSONObject data) {
        this.data = data;
    }

    public static Notification fromRemoteMessage(RemoteMessage message) {
        Notification instance = new Notification();
        JSONObject json = instance.data;

        if (message == null)
            return instance;

        try {
            Map<String, String> data = message.getData();
            JSONObject object = new JSONObject(data);
            Log.d("JSON_OBJECT", object.toString());

            json.put("message_id", message.getMessageId());
            json.put("title", object.getString("title"));
            json.put("body", object.getString("body"));
            json.put("date_sent", message.getSentTime());
            json.put("is_read", false);

            json.put("ttl",
                    data.containsKey("ttl")
                            ? data.get("ttl")
                            : 604800 * 4 // 4 weeks - default for FCM
            );

           /* json.put("category",
                data.containsKey("category")
                    ? data.get("category")
                    : JSONObject.NULL
            );*/

            json.put("buttons",
                    data.containsKey("buttons")
                            ? new JSONArray(data.get("buttons"))
                            : new JSONArray()
            );

            json.put("additional_data",
                    data.containsKey("additional_data")
                            ? new JSONObject(data.get("additional_data"))
                            : new JSONObject()
            );

            // compute when this notification will expire
        } catch (JSONException ignored) {
            Log.d(TAG, "remote message error!", ignored);
        }

        return instance;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.data.toString());
    }
}
