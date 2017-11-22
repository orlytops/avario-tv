package com.avariohome.avario.util;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

import com.avariohome.avario.exception.AvarioException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Loads assets and Maps them into specific Android view states (activated, selected, pressed,
 * default).
 *
 * This is not the class you want to use for warming up the cache
 *
 * Created by aeroheart-c6 on 02/03/2017.
 */
public class DrawableLoader extends AssetLoaderTask<Map<int[], Bitmap>> {
    public static final String TAG = "Avario/DrawableLoader";

    private static int[] stateDefault = new int[] {};
    private static int[][] states = new int[][] {
        new int[] {android.R.attr.state_activated},
        new int[] {android.R.attr.state_selected},
        new int[] {android.R.attr.state_pressed},
        stateDefault,
    };

    private Callback callback;

    public DrawableLoader(Context context, Callback callback) {
        super(context);
        this.callback = callback;
    }

    @Override
    protected Map<int[], Bitmap> processFetch(List<Bitmap> bitmaps) {
        return this.toStateMap(bitmaps);
    }

    @Override
    protected void onPostExecute(Map<int[], Bitmap> assets) {
        if (this.callback == null)
            return;

        if (this.exception == null)
            this.callback.onSuccess(this.getDrawable(assets));

        else
            this.callback.onFailure(this.exception);
    }

    @Override
    protected void onCancelled(Map<int[], Bitmap> assets) {
        if (this.callback != null)
            this.callback.onCancel();
    }

    private Map<int[], Bitmap> toStateMap(List<Bitmap> bitmaps) {
        Map<int[], Bitmap> mapping = new LinkedHashMap<>();

        if (bitmaps == null)
            mapping = null;
        else if (bitmaps.size() == 1)
            mapping.put(DrawableLoader.stateDefault, bitmaps.get(0));
        else
            for (int index = 0; index < DrawableLoader.states.length; index++)
                mapping.put(DrawableLoader.states[index], bitmaps.get(index));

        return mapping;
    }

    public Drawable getDrawable(Map<int[], Bitmap> assets) {
        if (assets == null)
            return AssetUtil.getPlaceholderDrawable(this.context);
        else if (assets.size() == 1) {
            Bitmap bitmap = assets.values().toArray(new Bitmap[0])[0];

            // return a BitmapDrawable
            return bitmap == null
                 ? AssetUtil.getPlaceholderDrawable(this.context)
                 : new BitmapDrawable(context.getResources(), bitmap);
        }
        else {
            // Return a StateListDrawable
            StateListDrawable states = new StateListDrawable();

            for (Map.Entry<int[], Bitmap> entry : assets.entrySet())
                states.addState(
                    entry.getKey(),
                    new BitmapDrawable(context.getResources(), entry.getValue())
                );

            return states;
        }
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Listeners
     ***********************************************************************************************
     */
    public interface Callback {
        void onSuccess(Drawable drawable);
        void onFailure(AvarioException exception);
        void onCancel();
    }
}
