package com.avariohome.avario.util;


/**
 * Created by aeroheart-c6 on 22/02/2017.
 */
public class Log {
    public static void i(String tag, String message, Throwable throwable) {

        android.util.Log.i(tag, message, throwable);
    }

    public static void i(String tag, String message) {
        Log.i(tag, message, null);
    }

    public static void d(String tag, String message, Throwable throwable) {

        android.util.Log.d(tag, message, throwable);
    }

    public static void d(String tag, String message) {
        Log.d(tag, message, null);
    }
}
