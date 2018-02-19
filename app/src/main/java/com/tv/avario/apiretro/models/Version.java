package com.tv.avario.apiretro.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by orly on 12/5/17.
 */

public class Version {

    @SerializedName("version")
    String version;

    @SerializedName("tablet")
    String tablet;

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public String getTablet() {
        return tablet;
    }
}
