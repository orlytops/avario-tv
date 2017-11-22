package com.avariohome.avario.util;


import android.content.Context;
import android.graphics.Bitmap;

import com.avariohome.avario.exception.AvarioException;

import java.util.List;


/**
 * Created by aeroheart-c6 on 02/03/2017.
 */
public class BlindAssetLoader extends AssetLoaderTask<Void> {
    private Callback callback;

    public BlindAssetLoader(Context context, Callback callback) {
        super(context);
        this.callback = callback;
    }

    @Override
    protected Void processFetch(List<Bitmap> bitmaps) {
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (this.callback == null)
            return;

        if (this.exception == null)
            this.callback.onSuccess();
        else
            this.callback.onFailure(this.exception);
    }

    @Override
    protected void onCancelled(Void result) {
        if (this.callback != null)
            this.callback.onCancel();
    }

    public interface Callback {
        void onSuccess();
        void onFailure(AvarioException exception);
        void onCancel();
    }
}
