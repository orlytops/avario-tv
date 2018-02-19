package com.tv.avario.apiretro.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by orly on 12/8/17.
 */

public class Updates {

    @SerializedName("versions")
    private Version version;

    public Version getVersion() {
        return version;
    }
}
