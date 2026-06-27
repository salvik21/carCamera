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

import com.example.cardvr.storage.FileManager;

import java.io.File;
import java.io.IOException;

public final class RecordingController {

    public interface Listener {
        void onRecordingStarted();

        void onRecordingStopping();

        void onRecordingSaved(@NonNull File file);

        void onRecordingError(@NonNull String message);
    }

    private final Context appContext;
    private final FileManager fileManager;
    private final Listener listener;

    private Recording activeRecording;
    private File activeFile;
    private boolean stopping;

    public RecordingController(
            @NonNull Context context,
            @NonNull FileManager fileManager,
            @NonNull Listener listener
    ) {
        appContext = context.getApplicationContext();
        this.fileManager = fileManager;
        this.listener = listener;
    }

    public void startRecording(
            @NonNull VideoCapture<Recorder> videoCapture,
            boolean recordAudio
    ) {
        if (activeRecording != null) {
            listener.onRecordingError("Запись уже запущена");
            return;
        }

        try {
            activeFile = fileManager.createVideoFile();
            FileOutputOptions outputOptions = new FileOutputOptions.Builder(activeFile).build();
            PendingRecording pendingRecording = videoCapture.getOutput()
                    .prepareRecording(appContext, outputOptions);
            if (recordAudio && ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED) {
                pendingRecording = pendingRecording.withAudioEnabled();
            }

            stopping = false;
            activeRecording = pendingRecording.start(
                    ContextCompat.getMainExecutor(appContext),
                    this::handleVideoRecordEvent
            );
        } catch (IOException | RuntimeException exception) {
            deleteEmptyActiveFile();
            clearRecordingState();
            listener.onRecordingError(buildErrorMessage("Не удалось начать запись", exception));
        }
    }

    public void stopRecording() {
        if (activeRecording == null || stopping) {
            return;
        }
        stopping = true;
        listener.onRecordingStopping();
        activeRecording.stop();
    }

    public boolean isRecording() {
        return activeRecording != null;
    }

    private void handleVideoRecordEvent(VideoRecordEvent event) {
        if (event instanceof VideoRecordEvent.Start) {
            listener.onRecordingStarted();
            return;
        }

        if (!(event instanceof VideoRecordEvent.Finalize)) {
            return;
        }

        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
        File completedFile = activeFile;
        clearRecordingState();

        if (isUsableSourceInactiveOutput(finalizeEvent, completedFile)) {
            listener.onRecordingSaved(completedFile);
        } else if (finalizeEvent.hasError()) {
            deleteFile(completedFile);
            Throwable cause = finalizeEvent.getCause();
            String details = cause == null ? "код " + finalizeEvent.getError()
                    : cause.getLocalizedMessage();
            listener.onRecordingError("Ошибка сохранения видео: " + details);
        } else if (completedFile != null) {
            listener.onRecordingSaved(completedFile);
        } else {
            listener.onRecordingError("CameraX завершил запись без файла");
        }
    }

    private boolean isUsableSourceInactiveOutput(
            VideoRecordEvent.Finalize finalizeEvent,
            File completedFile
    ) {
        return finalizeEvent.getError() == VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
                && completedFile != null
                && completedFile.isFile()
                && completedFile.length() > 0;
    }

    private void clearRecordingState() {
        activeRecording = null;
        activeFile = null;
        stopping = false;
    }

    private void deleteEmptyActiveFile() {
        deleteFile(activeFile);
    }

    private void deleteFile(File file) {
        if (file != null && file.exists() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    private String buildErrorMessage(String prefix, Exception exception) {
        String details = exception.getLocalizedMessage();
        return details == null || details.trim().isEmpty() ? prefix : prefix + ": " + details;
    }
}
