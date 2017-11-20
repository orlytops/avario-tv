package com.avario.home.bus;

/**
 * Created by orly on 10/6/17.
 */

public class OnKioskMode {

    private boolean isKiosk;

    public OnKioskMode(boolean isKiosk) {
        this.isKiosk = isKiosk;
    }

    public boolean isKiosk() {
        return isKiosk;
    }
}
