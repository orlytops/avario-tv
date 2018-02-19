package com.tv.avario.core;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;


/**
 * Created by aeroheart-c6 on 19/02/2017.
 */
public class WorkerThread extends HandlerThread {
    private Handler handler;

    public WorkerThread() {
        super("AvarioWorker", Process.THREAD_PRIORITY_BACKGROUND);
    }

    public Handler getHandler() {
        if (!this.isAlive())
            return null;

        if (this.handler == null)
            this.handler = new Handler(this.getLooper());

        return this.handler;
    }
}
