package com.example.cardvr.recording;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;

import com.example.cardvr.database.OperationStatus;
import com.example.cardvr.database.SegmentStatus;
import com.example.cardvr.database.VideoSegmentEntity;
import com.example.cardvr.location.VideoMetadataProvider;
import com.example.cardvr.location.VideoMetadataSnapshot;
import com.example.cardvr.protection.ProtectionManager;
import com.example.cardvr.settings.SettingsRepository;
import com.example.cardvr.storage.FileCleanupManager;
import com.example.cardvr.storage.FileManager;
import com.example.cardvr.storage.StorageLimitManager;

import java.io.File;

public final class SegmentManager implements SegmentRecorder.Listener {
    public interface Listener {
        void onSessionStarted();
        void onSegmentCompleted(@NonNull File file);
        void onSessionStopped(File lastFile);
        void onFatalError(@NonNull String message);
        void onStorageExhausted();
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final SegmentRepository repository;
    private final SettingsRepository settings;
    private final FileManager files;
    private final StorageLimitManager limits;
    private final FileCleanupManager cleanup;
    private final ProtectionManager protection;
    private final SegmentRecorder recorder;
    private final Listener listener;

    private VideoCapture<Recorder> capture;
    private boolean audio;
    private String cameraId;
    private volatile boolean sessionActive;
    private volatile boolean stopRequested;
    private VideoSegmentEntity current;
    private File lastFile;
    private volatile Long tripId;
    private volatile VideoMetadataProvider metadataProvider;
    private volatile double segmentMaxSpeed;

    public void setTripContext(Long tripId, VideoMetadataProvider provider) {
        this.tripId = tripId;
        this.metadataProvider = provider;
    }

    public void onMetadata(VideoMetadataSnapshot snapshot) {
        if (snapshot != null) segmentMaxSpeed = Math.max(segmentMaxSpeed, snapshot.speedKmh);
    }

    public SegmentManager(Context context, SegmentRepository repository,
                          SettingsRepository settings, StorageLimitManager limits,
                          FileCleanupManager cleanup, ProtectionManager protection,
                          Listener listener) {
        this.repository = repository;
        this.settings = settings;
        this.limits = limits;
        this.cleanup = cleanup;
        this.protection = protection;
        this.listener = listener;
        files = new FileManager(context);
        recorder = new SegmentRecorder(context, this);
    }

    public void start(VideoCapture<Recorder> capture, boolean audio, String cameraId) {
        if (sessionActive) return;
        this.capture = capture;
        this.audio = audio;
        this.cameraId = cameraId;
        sessionActive = true;
        stopRequested = false;
        createAndStartNext();
    }

    public void stop() {
        stopRequested = true;
        main.removeCallbacksAndMessages(null);
        if (recorder.isRecording()) recorder.stop();
        else finishSession();
    }

    public boolean isRecording() {
        return sessionActive && recorder.isRecording();
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public void protectWindow() {
        SegmentRepository.ioExecutor().execute(protection::protectWindow);
    }

    private void createAndStartNext() {
        SegmentRepository.ioExecutor().execute(() -> {
            try {
                if (!sessionActive || stopRequested) return;
                if (!limits.hasEnoughFreeSpace()
                        && cleanup.cleanupToLimits() != FileCleanupManager.Result.SATISFIED) {
                    sessionActive = false;
                    main.post(listener::onStorageExhausted);
                    return;
                }
                File file = files.createVideoFile();
                long now = System.currentTimeMillis();
                VideoSegmentEntity entity = new VideoSegmentEntity();
                entity.fileName = file.getName();
                entity.filePath = file.getAbsolutePath();
                entity.startTime = now;
                entity.createdAt = now;
                entity.cameraId = cameraId;
                entity.status = SegmentStatus.RECORDING;
                entity.uploadStatus = OperationStatus.NONE;
                entity.transferStatus = OperationStatus.NONE;
                entity.id = repository.insert(entity);
                VideoMetadataSnapshot startMetadata = metadataProvider == null
                        ? null : metadataProvider.getSnapshot();
                entity.tripId = tripId;
                if (startMetadata != null) {
                    entity.startLatitude = startMetadata.latitude;
                    entity.startLongitude = startMetadata.longitude;
                }
                repository.attachToTrip(entity.id, entity.tripId,
                        entity.startLatitude, entity.startLongitude);
                segmentMaxSpeed = startMetadata == null ? 0 : startMetadata.speedKmh;
                current = entity;
                protection.onSegmentCreated(entity.id);
                main.post(() -> {
                    if (!sessionActive || stopRequested) return;
                    try {
                        recorder.start(capture, file, audio);
                    } catch (RuntimeException error) {
                        failCurrent(error);
                    }
                });
            } catch (Exception error) {
                main.post(() -> listener.onFatalError(message("Cannot create segment", error)));
            }
        });
    }

    @Override
    public void onStarted() {
        listener.onSessionStarted();
        main.postDelayed(() -> {
            if (sessionActive && !stopRequested) recorder.stop();
        }, settings.getSegmentDurationMs());
    }

    @Override
    public void onFinalized(@NonNull File file, boolean success, int error, Throwable cause) {
        main.removeCallbacksAndMessages(null);
        VideoSegmentEntity completed = current;
        current = null;
        lastFile = file;
        SegmentRepository.ioExecutor().execute(() -> {
            if (completed != null) {
                long now = System.currentTimeMillis();
                completed.endTime = now;
                completed.durationMs = Math.max(0L, now - completed.startTime);
                completed.sizeBytes = file.isFile() ? file.length() : 0L;
                completed.status = success
                        ? (protection.shouldProtectOnFinalize(completed.id)
                        ? SegmentStatus.PROTECTED : SegmentStatus.NORMAL)
                        : SegmentStatus.ERROR;
                VideoMetadataSnapshot endMetadata = metadataProvider == null
                        ? null : metadataProvider.getSnapshot();
                if (endMetadata != null) {
                    completed.endLatitude = endMetadata.latitude;
                    completed.endLongitude = endMetadata.longitude;
                }
                completed.maxSpeedKmh = segmentMaxSpeed;
                repository.update(completed);
            }
            if (success) main.post(() -> listener.onSegmentCompleted(file));
            if (!success) {
                sessionActive = false;
                String detail = cause == null ? "CameraX error " + error
                        : message("CameraX error " + error, cause);
                main.post(() -> listener.onFatalError(detail));
                return;
            }
            if (stopRequested || !sessionActive) {
                main.post(this::finishSession);
                return;
            }
            main.post(this::createAndStartNext);
            FileCleanupManager.Result result = cleanup.cleanupToLimits();
            if (result != FileCleanupManager.Result.SATISFIED && !limits.hasEnoughFreeSpace()) {
                stopRequested = true;
                main.post(() -> {
                    if (recorder.isRecording()) recorder.stop();
                    else listener.onStorageExhausted();
                });
            }
        });
    }

    private void failCurrent(Throwable error) {
        sessionActive = false;
        VideoSegmentEntity failed = current;
        current = null;
        SegmentRepository.ioExecutor().execute(() -> {
            if (failed != null) {
                failed.status = SegmentStatus.ERROR;
                failed.endTime = System.currentTimeMillis();
                repository.update(failed);
            }
            main.post(() -> listener.onFatalError(message("Cannot start segment", error)));
        });
    }

    private void finishSession() {
        sessionActive = false;
        listener.onSessionStopped(lastFile);
    }

    private static String message(String prefix, Throwable error) {
        String detail = error.getLocalizedMessage();
        return detail == null ? prefix : prefix + ": " + detail;
    }
}
