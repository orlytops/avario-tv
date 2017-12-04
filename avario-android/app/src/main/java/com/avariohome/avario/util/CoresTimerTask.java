package com.avariohome.avario.util;

import java.util.TimerTask;

public class CoresTimerTask extends TimerTask {

    private boolean hasStarted = false;


    public boolean hasRunStarted() {
        return this.hasStarted;
    }

    @Override
    public void run() {
        this.hasStarted = true;
    }
}