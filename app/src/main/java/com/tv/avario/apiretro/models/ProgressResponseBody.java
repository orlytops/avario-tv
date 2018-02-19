package com.tv.avario.apiretro.models;

import android.os.RecoverySystem;

import com.tv.avario.bus.UpdateDownload;
import com.tv.avario.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class ProgressResponseBody extends ResponseBody {

    private final ResponseBody responseBody;
    private final RecoverySystem.ProgressListener progressListener;
    private BufferedSource bufferedSource;

    public ProgressResponseBody(ResponseBody responseBody, RecoverySystem.ProgressListener progressListener) {
        this.responseBody = responseBody;
        this.progressListener = progressListener;
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(final Source source) {
        return new ForwardingSource(source) {
            long totalBytesRead = 0L;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                Log.d("Bytes", totalBytesRead + " " + responseBody.contentLength() + " ");

                int percent = (int) ((totalBytesRead * 100) / responseBody.contentLength());
                Log.d("SUB TYPE", responseBody.contentType().subtype() + " " + responseBody.contentType().type());
                if (responseBody.contentType().subtype().equals("octet-stream")) {
                    EventBus.getDefault().post(new UpdateDownload(percent));
                }
                return bytesRead;
            }
        };
    }
}