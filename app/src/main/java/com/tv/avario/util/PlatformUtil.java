package com.tv.avario.util;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.tv.avario.core.StateArray;
import com.tv.avario.exception.AvarioException;

import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.List;


/**
 * Created by aeroheart-c6 on 06/01/2017.
 */
public class PlatformUtil {
    private static final String TAG = "Avario/Platform";
    private static final char[] hexchars = "0123456789abcdef".toCharArray();

    private static SimpleDateFormat formatter = null;

    public static boolean isLollipopOrNewer() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static String getTabletId() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception exception) {}
        return "";
    }


    public static String hashMD5(String string) {
        try {
            MessageDigest digest;
            byte[] message;
            char[] chars;

            digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(string.getBytes(Charset.forName("UTF-8")));
            message = digest.digest();

            chars = new char[message.length * 2];

            for (int index = 0; index < message.length; index++) {
                byte tmp = message[index];
                int item = tmp & 0xff;

                chars[index * 2] = PlatformUtil.hexchars[item >>> 4];
                chars[index * 2 + 1] = PlatformUtil.hexchars[item & 0x0f];
            }

            return new String(chars);
        }
        catch (NoSuchAlgorithmException exception) {
            return string;
        }
    }

    @SuppressLint("DefaultLocale")
    public static String toDateTimeString(long seconds) {
        return DateFormat.getDateTimeInstance().format(new Date(seconds));
    }

    @SuppressLint("DefaultLocale")
    public static String toTimeString(long seconds) {
        return DateUtils.formatElapsedTime(seconds);
    }

    public static long toTimestamp(String dateString) {
        if (PlatformUtil.formatter == null)
            PlatformUtil.formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZ");

        try {
            return PlatformUtil.formatter.parse(dateString).getTime();
        }
        catch (ParseException exception) {
            return 0;
        }
    }

    public static int responseCodeToErrorCode(String prefix, int httpCode) {
        return Integer.parseInt(prefix + httpCode, 16);
    }

    public static AlertDialog getErrorDialog(Context context, AvarioException exception) {
        String message = PlatformUtil.logError(exception);

        return new AlertDialog.Builder(context)
            .setMessage(message)
            .create();
    }

    public static Toast getErrorToast(Context context, AvarioException exception) {
        String message = PlatformUtil.logError(exception);

        return Toast.makeText(context, message, Toast.LENGTH_LONG);
    }

    public static String logError(AvarioException exception) {
        String message = StateArray
            .getInstance()
            .getErrorMessage(exception.getCode());

        try {
            message = String.format(message, exception.getMessageArguments());
        }
        catch (IllegalFormatException formatExc) {}

        Log.i(
            TAG, String.format("Application Error Occurred\n%s", message),
            exception.getCause()
        );

        return message;
    }

    public static class ErrorToastRunnable implements Runnable {
        private AvarioException exception;
        private Context context;

        public ErrorToastRunnable(Context context, AvarioException exception) {
            this.exception = exception;
            this.context = context;
        }

        @Override
        public void run() {
            PlatformUtil
                .getErrorToast(this.context, this.exception)
                .show();
        }
    }
}
