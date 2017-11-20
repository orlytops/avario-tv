package com.avario.home.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by orly on 10/18/17.
 */

public class SaturationBody {

    @SerializedName("entity_id")
    private String entityId;

    @SerializedName("white_value")
    private int whiteValue;

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setWhiteValue(int whiteValue) {
        this.whiteValue = whiteValue;
    }
}
