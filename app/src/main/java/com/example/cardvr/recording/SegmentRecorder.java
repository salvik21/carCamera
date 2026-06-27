package com.example.cardvr.recording;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.content.ContextCompat;

import java.io.File;

public final class SegmentRecorder {
    public interface Listener {
        void onStarted();
        void onFinalized(@NonNull File file, boolean success, int error, Throwable cause);
    }

    private final Context context;
    private final Listener listener;
    private Recording recording;
    private File file;

    public SegmentRecorder(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void start(VideoCapture<Recorder> capture, File output, boolean audio) {
        if (recording != null) throw new IllegalStateException("Segment already active");
        file = output;
        PendingRecording pending = capture.getOutput().prepareRecording(context,
                new FileOutputOptions.Builder(output).build());
        if (audio && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            pending = pending.withAudioEnabled();
        }
        recording = pending.start(ContextCompat.getMainExecutor(context), this::onEvent);
    }

    public void stop() {
        if (recording != null) recording.stop();
    }

    public boolean isRecording() {
        return recording != null;
    }

    private void onEvent(VideoRecordEvent event) {
        if (event instanceof VideoRecordEvent.Start) {
            listener.onStarted();
        } else if (event instanceof VideoRecordEvent.Finalize) {
            VideoRecordEvent.Finalize finalized = (VideoRecordEvent.Finalize) event;
            File completed = file;
            recording = null;
            file = null;
            boolean usable = !finalized.hasError()
                    || finalized.getError() == VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE;
            usable = usable && completed != null && completed.isFile() && completed.length() > 0;
            listener.onFinalized(completed, usable, finalized.getError(), finalized.getCause());
        }
    }
}
