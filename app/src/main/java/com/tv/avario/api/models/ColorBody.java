package com.tv.avario.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by orly on 10/18/17.
 */

public class ColorBody {

    @SerializedName("entity_id")
    private String entityId;

    @SerializedName("rgb_color")
    private int[] rgbColor;

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setRgbColor(int[] rgbColor) {
        this.rgbColor = rgbColor;
    }
}
