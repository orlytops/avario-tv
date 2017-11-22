package com.avariohome.avario.core;


import android.app.Application;
import android.content.Context;

import com.avariohome.avario.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Stores notifications from FCM into a file that is represented here, in this class. Naming for
 * slight consistency with StateArray
 *
 * Created by aeroheart-c6 on 7/11/17.
 */
public class NotificationArray {
    private static final String TAG = "Avario/NotifsArray";

    private static final NotificationArray instance = new NotificationArray();

    public static NotificationArray getInstance() {
        return NotificationArray.instance;
    }


    private Context context;
    private List<Notification> data;

    private NotificationArray() {
        this.data = new ArrayList<>();
    }

    public void setContext(Context context) {
        this.context = context instanceof Application
                     ? context
                     : context.getApplicationContext();
    }

    /**
     * Saves the notification into the data structure
     *
     * @param notification
     */
    public void addNotification(Notification notification) {
        this.data.add(0, notification);
    }

    /**
     * Deletes the notificiation provided. Searches the notification array and matches by messageId
     * from FCM
     */
    public void deleteNotification(Notification notification) {
        Iterator<Notification> iterator = this.data.iterator();
        String messageId = notification.data.optString("message_id");

        while (iterator.hasNext()) {
            Notification savedNotif = iterator.next();
            String savedId = savedNotif.data.optString("message_id");

            if (messageId.equals(savedId)) {
                iterator.remove();
                break;
            }
        }
    }


    /**
     * Traverses into the data structure and removes the items that have expired based on their
     * ttl value
     */
    public void deleteExpired() {
        Iterator<Notification> iterator = this.data.iterator();
        long now = System.currentTimeMillis();

        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            long expires = notification.data.optLong("date_sent") + notification.data.optLong("ttl");

            if (now >= expires) {
                Log.d(TAG, "removing...");
                iterator.remove();
            }
        }
    }

    public List<Notification> getNotifications() {
        if (this.data == null)
            return new ArrayList<>();

        return this.data;
    }


    // region file operations
    /**
     * Unused because we are no longer persisting the data into disk. Commenting out in case we
     * may want it back
     */
    /*
    public void delete() {
        File file = this.getFile();

        if (!file.exists())
            return;

        try {
            file.delete();
        }
        catch (SecurityException exception) {
            Log.d(TAG, "Error occurred", exception);
        }
    }

    public void save() {
        File file = this.getFile();
        JSONArray notifsJSON;

        if (this.data == null)
            return;

        notifsJSON = new JSONArray();

        for (Notification notification : this.data)
            notifsJSON.put(notification.data);

        try {
            BufferedWriter writer;

            writer = new BufferedWriter(new FileWriter(file));
            writer.write(notifsJSON.toString());
            writer.close();

            Log.d(TAG, "Saved to: " + file.getAbsolutePath());
        }
        catch (IOException ignored) {}
    }

    public void load() {
        File file = this.getFile();

        this.data = new ArrayList<>();

        if (file == null || !file.exists())
            return;

        StringBuilder builder = new StringBuilder();

        try {
            BufferedReader stream = new BufferedReader(new FileReader(file));
            String line;

            while ((line = stream.readLine()) != null)
                builder.append(line);

            stream.close();
        }
        catch (IOException exception) {
            Log.d(TAG, "Unexpected error occurred", exception);
            // only happens when reading is done after closing the stream. Not happening, I assume.
        }

        try {
            JSONArray notifsJSON = new JSONArray(builder.toString());

            for (int index = 0, length = notifsJSON.length(); index < length; index++)
                this.data.add(new Notification(notifsJSON.optJSONObject(index)));
        }
        catch (JSONException ignored) {}
    }

    public void clear() {
        if (data == null)
            data = new ArrayList<>();

        this.data.clear();
    }

    private File getFile() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return null;

        File file = new File(this.context.getExternalFilesDir(null), this.context.getString(R.string.app__path__notif)),
             dirs = file.getParentFile();

        if (!dirs.exists())
            dirs.mkdirs();

        return file;
    }
    */
    // endregion
}