package com.tv.avario.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by orly on 10/23/17.
 */

public class TemperatureBody {

    @SerializedName("entity_id")
    private String entityId;

    @SerializedName("color_temp")
    private int colorTemp;

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setColorTemp(int colorTemp) {
        this.colorTemp = colorTemp;
    }
}
