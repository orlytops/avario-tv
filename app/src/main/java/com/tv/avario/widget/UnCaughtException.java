package com.tv.avario.widget;

/**
 * Created by orly on 12/1/17.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;

public class UnCaughtException implements UncaughtExceptionHandler {


    private Context context;
    private static Context context1;

    public UnCaughtException(Context ctx) {
        context = ctx;
        context1 = ctx;
    }

    private StatFs getStatFs() {
        File path = Environment.getDataDirectory();
        return new StatFs(path.getPath());
    }

    private long getAvailableInternalMemorySize(StatFs stat) {
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    private long getTotalInternalMemorySize(StatFs stat) {
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    private void addInformation(StringBuilder message) {
        message.append("Locale: ").append(Locale.getDefault()).append('\n');
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi;
            pi = pm.getPackageInfo(context.getPackageName(), 0);
            message.append("Version: ").append(pi.versionName).append('\n');
            message.append("Package: ").append(pi.packageName).append('\n');
        } catch (Exception e) {
            Log.e("CustomExceptionHandler", "Error", e);
            message.append("Could not get Version information for ").append(
                    context.getPackageName());
        }
        message.append("Phone Model: ").append(android.os.Build.MODEL)
                .append('\n');
        message.append("Android Version: ")
                .append(android.os.Build.VERSION.RELEASE).append('\n');
        message.append("Board: ").append(android.os.Build.BOARD).append('\n');
        message.append("Brand: ").append(android.os.Build.BRAND).append('\n');
        message.append("Device: ").append(android.os.Build.DEVICE).append('\n');
        message.append("Host: ").append(android.os.Build.HOST).append('\n');
        message.append("ID: ").append(android.os.Build.ID).append('\n');
        message.append("Model: ").append(android.os.Build.MODEL).append('\n');
        message.append("Product: ").append(android.os.Build.PRODUCT)
                .append('\n');
        message.append("Type: ").append(android.os.Build.TYPE).append('\n');
        StatFs stat = getStatFs();
        message.append("Total Internal memory: ")
                .append(getTotalInternalMemorySize(stat)).append('\n');
        message.append("Available Internal memory: ")
                .append(getAvailableInternalMemorySize(stat)).append('\n');
    }

    public void uncaughtException(Thread t, Throwable e) {
        try {
            sendErrorMail(e.getMessage());
        } catch (Throwable ignore) {
            Log.e(UnCaughtException.class.getName(),
                    "Error while sending error e-mail", ignore);
        }
    }

    /**
     * This method for call alert dialog when application crashed!
     */
    public void sendErrorMail(final String message) {

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();

                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Crash Error");
                builder.create();
                builder.setNegativeButton("Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(1);
                            }
                        });
                builder.setMessage(message);
                builder.show();
                Looper.loop();
            }
        }.start();
    }
}
