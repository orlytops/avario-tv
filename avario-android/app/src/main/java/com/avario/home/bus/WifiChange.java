package com.avario.home.bus;

/**
 * Created by orly on 10/10/17.
 */

public class WifiChange {
    private boolean isConnected;
    private boolean isAuthError;

    public WifiChange(boolean isAuthError){
        this.isAuthError = isAuthError;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isAuthError() {
        return isAuthError;
    }
}
