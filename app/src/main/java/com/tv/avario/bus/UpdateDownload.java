package com.tv.avario.bus;

/**
 * Created by orly on 12/5/17.
 */

public class UpdateDownload {

    private int progress;

    public UpdateDownload(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
