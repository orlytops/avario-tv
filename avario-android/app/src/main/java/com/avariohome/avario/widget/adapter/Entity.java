package com.avariohome.avario.widget.adapter;


import android.graphics.drawable.Drawable;

import org.json.JSONObject;


/**
 * A common class used to store data about an entity being represented in an Adapter. This is NOT
 * to be confused by the actual Entity data from the StateArray.
 * <p>
 * <p>
 * ...I just named it this way for lack of short class names >.<
 * <p>
 * Created by aeroheart-c6 on 12/04/2017.
 */
public class Entity {
    public JSONObject data;
    public String id;
    public boolean selected;
    public Drawable holder;
}
