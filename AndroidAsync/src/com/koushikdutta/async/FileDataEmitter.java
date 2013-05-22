package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by koush on 5/22/13.
 */
public class FileDataEmitter implements DataEmitter {
    AsyncServer server;
    File file;
    public FileDataEmitter(AsyncServer server, File file) {
        this.server = server;
        this.file = file;
        doResume();
    }

    DataCallback callback;
    @Override
    public void setDataCallback(DataCallback callback) {
        this.callback = callback;
    }

    @Override
    public DataCallback getDataCallback() {
        return callback;
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    boolean paused;
    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    private void report(Exception e) {
        try {
            channel.close();
        }
        catch (Exception ex) {
            ex = e;
        }
        if (endCallback != null)
            endCallback.onCompleted(e);
    }

    ByteBufferList pending = new ByteBufferList();
    FileChannel channel;
    Runnable pumper = new Runnable() {
        @Override
        public void run() {
            try {
                if (channel == null)
                    channel = new FileInputStream(file).getChannel();
                if (!pending.isEmpty()) {
                    Util.emitAllData(FileDataEmitter.this, pending);
                    if (!pending.isEmpty())
                        return;
                }
                ByteBuffer b;
                do {
                    b = ByteBuffer.allocate(8192);
                    if (-1 == channel.read(b)) {
                        report(null);
                        return;
                    }
                    b.flip();
                    pending.add(b);
                    Util.emitAllData(FileDataEmitter.this, pending);
                }
                while (pending.remaining() == 0 && !isPaused());
            }
            catch (Exception e) {
                report(e);
            }
        }
    };

    private void doResume() {
        server.post(pumper);
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    CompletedCallback endCallback;
    @Override
    public void setEndCallback(CompletedCallback callback) {
        endCallback = callback;
    }

    @Override
    public CompletedCallback getEndCallback() {
        return endCallback;
    }

    @Override
    public AsyncServer getServer() {
        return server;
    }
}