package com.avario.home.bus;

/**
 * Created by orly on 10/11/17.
 */

public class WifiConnected {

    private boolean isConnected;

    public WifiConnected(boolean isConnected){
        this.isConnected = isConnected;
    }

    public boolean isConnected() {
        return isConnected;
    }


}
