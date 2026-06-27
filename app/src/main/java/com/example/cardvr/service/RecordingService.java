package com.example.cardvr.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.example.cardvr.power.BatteryMonitor;
import com.example.cardvr.power.ChargingStateManager;
import com.example.cardvr.protection.ProtectionManager;
import com.example.cardvr.recording.RecordingRecoveryManager;
import com.example.cardvr.recording.SegmentManager;
import com.example.cardvr.recording.SegmentRepository;
import com.example.cardvr.recording.RecordingStateManager;
import com.example.cardvr.settings.SettingsRepository;
import com.example.cardvr.storage.FileCleanupManager;
import com.example.cardvr.storage.StorageLimitManager;
import com.example.cardvr.storage.StorageManager;
import com.example.cardvr.database.TripEntity;
import com.example.cardvr.database.TripStatus;
import com.example.cardvr.location.LocationFilter;
import com.example.cardvr.location.LocationTracker;
import com.example.cardvr.location.SpeedCalculator;
import com.example.cardvr.location.VideoMetadataProvider;
import com.example.cardvr.location.VideoMetadataSnapshot;
import com.example.cardvr.trips.TripManager;
import com.example.cardvr.trips.TripRepository;
import com.example.cardvr.trips.TripState;
import com.example.cardvr.trips.TripEndDetector;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;

public final class RecordingService extends Service implements
        LifecycleOwner,
        SegmentManager.Listener,
        LocationTracker.Listener,
        VideoMetadataProvider,
        ChargingStateManager.Listener {

    public static final String ACTION_START =
            "com.example.cardvr.action.START_RECORDING";
    public static final String ACTION_STOP =
            "com.example.cardvr.action.STOP_RECORDING";
    public static final String ACTION_PROTECT =
            "com.example.cardvr.action.PROTECT_RECORDING";
    public static final String ACTION_END_TRIP =
            "com.example.cardvr.action.END_TRIP";
    public static final String EXTRA_LENS_FACING = "extra_lens_facing";

    private static final String TAG = "RecordingService";
    private static final long TIMER_INTERVAL_MILLIS = 1_000L;

    private final LocalBinder binder = new LocalBinder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            RecordingStateManager.State state = stateManager.getCurrentState();
            if (state.getStatus() != RecordingStateManager.Status.RECORDING) {
                return;
            }
            long elapsed = SystemClock.elapsedRealtime()
                    - state.getStartedAtElapsedRealtime();
            stateManager.setElapsed(elapsed);
            notificationManager.update(stateManager.getCurrentState());
            mainHandler.postDelayed(this, TIMER_INTERVAL_MILLIS);
        }
    };

    private RecordingStateManager stateManager;
    private ChargingStateManager chargingStateManager;
    private BatteryMonitor batteryMonitor;
    private RecordingNotificationManager notificationManager;
    private SegmentManager segmentManager;
    private SegmentRepository segmentRepository;
    private SettingsRepository settingsRepository;
    private TripManager tripManager;
    private LocationTracker locationTracker;
    private final LocationFilter locationFilter = new LocationFilter();
    private final SpeedCalculator speedCalculator = new SpeedCalculator();
    private volatile VideoMetadataSnapshot metadataSnapshot;
    private final TripEndDetector tripEndDetector = new TripEndDetector();
    private volatile long lastMovementAt = System.currentTimeMillis();
    private volatile long powerDisconnectedAt;
    private volatile boolean charging;

    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private Preview.SurfaceProvider previewSurfaceProvider;
    private boolean sessionStarted;
    private boolean stopping;
    private boolean finishing;
    private boolean foregroundStarted;
    private int cameraRequestId;
    private PowerManager.WakeLock recordingWakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        stateManager = RecordingStateManager.getInstance();
        chargingStateManager = ChargingStateManager.getInstance();
        batteryMonitor = new BatteryMonitor(this);
        notificationManager = new RecordingNotificationManager(this);
        SegmentRepository repository = new SegmentRepository(this);
        segmentRepository = repository;
        SettingsRepository settings = new SettingsRepository(this);
        settingsRepository = settings;
        StorageManager storage = new StorageManager(this);
        StorageLimitManager limits = new StorageLimitManager(settings, storage);
        FileCleanupManager cleanup = new FileCleanupManager(repository, storage, limits);
        ProtectionManager protection = new ProtectionManager(repository);
        segmentManager = new SegmentManager(this, repository, settings, limits,
                cleanup, protection, this);
        locationTracker = new LocationTracker(this, this);
        SegmentRepository.ioExecutor().execute(() ->
                tripManager = new TripManager(new TripRepository(this), repository));
        SegmentRepository.ioExecutor().execute(
                () -> new RecordingRecoveryManager(repository).recover());
        chargingStateManager.addListener(this);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action) || ACTION_END_TRIP.equals(action)) {
            requestStop();
            return START_NOT_STICKY;
        }
        if (ACTION_PROTECT.equals(action)) {
            markProtectedEvent();
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(action)) {
            try {
                startForegroundImmediately();
                foregroundStarted = true;
            } catch (RuntimeException exception) {
                stateManager.setError(buildErrorMessage(
                        "Не удалось запустить foreground service", exception));
                stopSelf();
                return START_NOT_STICKY;
            }
            int lensFacing = intent.getIntExtra(
                    EXTRA_LENS_FACING,
                    CameraSelector.LENS_FACING_BACK
            );
            startRecordingSession(lensFacing);
        }
        return START_NOT_STICKY;
    }

    private void startRecordingSession(@CameraSelector.LensFacing int lensFacing) {
        if (sessionStarted || finishing) {
            Log.i(TAG, "Повторный запрос запуска проигнорирован");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            stateManager.setError("Нет разрешения камеры");
            stopSelf();
            return;
        }

        String cameraName = lensFacing == CameraSelector.LENS_FACING_FRONT
                ? "Фронтальная"
                : "Задняя";
        if (!stateManager.trySetStarting(cameraName)) {
            stopSelf();
            return;
        }

        sessionStarted = true;
        ChargingStateManager.ChargingState chargingState =
                BatteryMonitor.readCurrentState(this);
        chargingStateManager.updateState(chargingState);
        batteryMonitor.start();

        try {
            if (!foregroundStarted) {
                startForegroundImmediately();
                foregroundStarted = true;
            }
            acquireRecordingWakeLock();
        } catch (RuntimeException exception) {
            stateManager.setError(buildErrorMessage(
                    "Не удалось запустить foreground service",
                    exception
            ));
            finishService(false);
            return;
        }
        openCameraAndStartRecording(lensFacing);
    }

    @SuppressLint("WakelockTimeout")
    private void acquireRecordingWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        recordingWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                getPackageName() + ":RecordingWakeLock"
        );
        recordingWakeLock.setReferenceCounted(false);
        recordingWakeLock.acquire();
    }

    private void releaseRecordingWakeLock() {
        if (recordingWakeLock != null && recordingWakeLock.isHeld()) {
            recordingWakeLock.release();
        }
        recordingWakeLock = null;
    }

    private void startForegroundImmediately() {
        Notification notification = notificationManager.createNotification(
                stateManager.getCurrentState()
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int serviceTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED) {
                serviceTypes |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                serviceTypes |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            }
            startForeground(
                    RecordingNotificationManager.NOTIFICATION_ID,
                    notification,
                    serviceTypes
            );
        } else {
            startForeground(RecordingNotificationManager.NOTIFICATION_ID, notification);
        }
    }

    private void openCameraAndStartRecording(
            @CameraSelector.LensFacing int lensFacing
    ) {
        int requestId = ++cameraRequestId;
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            if (requestId != cameraRequestId || stopping || finishing) {
                return;
            }
            try {
                cameraProvider = providerFuture.get();
                bindCamera(lensFacing);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                handleCameraError("Не удалось открыть камеру",
                        cause == null ? exception : cause);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                handleCameraError("Открытие камеры прервано", exception);
            } catch (RuntimeException exception) {
                handleCameraError("Ошибка CameraX", exception);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(@CameraSelector.LensFacing int lensFacing) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();
        try {
            if (!cameraProvider.hasCamera(cameraSelector)) {
                throw new IllegalStateException("Выбранная камера отсутствует");
            }
        } catch (CameraInfoUnavailableException exception) {
            throw new IllegalStateException("Не удалось получить сведения о камере", exception);
        }

        preview = new Preview.Builder().build();
        if (previewSurfaceProvider != null) {
            preview.setSurfaceProvider(previewSurfaceProvider);
            stateManager.setPreviewAttached(true);
        }

        QualitySelector qualitySelector = QualitySelector.from(
                Quality.HD,
                FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
        );
        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build();
        VideoCapture<Recorder> videoCapture = VideoCapture.withOutput(recorder);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                videoCapture
        );
        boolean recordAudio = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
        SegmentRepository.ioExecutor().execute(() -> {
            if (tripManager == null) {
                tripManager = new TripManager(new TripRepository(this), segmentRepository);
            }
            TripEntity trip = tripManager.startTrip();
            segmentManager.setTripContext(trip.id, this);
            mainHandler.post(() -> {
                long interval = settingsRepository.getGpsIntervalMs();
                int battery = stateManager.getCurrentState().getBatteryPercent();
                if (battery >= 0 && battery < 15
                        && settingsRepository.reduceGpsOnLowBattery()) {
                    interval = 5_000L;
                }
                locationTracker.start(interval, settingsRepository.getGpsPriority());
                segmentManager.start(videoCapture, recordAudio, String.valueOf(lensFacing));
            });
        });
    }

    public void attachPreview(@NonNull Preview.SurfaceProvider surfaceProvider) {
        mainHandler.post(() -> {
            previewSurfaceProvider = surfaceProvider;
            if (preview != null) {
                preview.setSurfaceProvider(surfaceProvider);
                stateManager.setPreviewAttached(true);
            }
        });
    }

    public void detachPreview() {
        mainHandler.post(() -> {
            previewSurfaceProvider = null;
            if (preview != null) {
                preview.setSurfaceProvider(null);
            }
            stateManager.setPreviewAttached(false);
        });
    }

    private void requestStop() {
        if (stopping || finishing) {
            return;
        }
        stopping = true;
        if (locationTracker != null) locationTracker.stop();
        stateManager.setStopping();
        mainHandler.removeCallbacks(timerRunnable);
        if (segmentManager.isSessionActive()) {
            segmentManager.stop();
        } else {
            finishService(true);
        }
    }

    private void markProtectedEvent() {
        if (!stateManager.getCurrentState().isServiceActive()) {
            return;
        }
        Log.i(TAG, "Пользователь отметил текущую запись как защищённую (заглушка)");
        segmentManager.protectWindow();
        stateManager.markProtectedEvent();
        notificationManager.update(stateManager.getCurrentState());
    }

    @Override
    public void onSessionStarted() {
        if (stateManager.getCurrentState().getStatus()
                != RecordingStateManager.Status.RECORDING) {
            stateManager.setRecording(SystemClock.elapsedRealtime());
        }
        notificationManager.update(stateManager.getCurrentState());
        mainHandler.removeCallbacks(timerRunnable);
        mainHandler.post(timerRunnable);
    }

    @Override
    public void onSegmentCompleted(@NonNull File file) {
        Log.i(TAG, "Segment saved: " + file.getName());
    }

    @Override
    public void onSessionStopped(@Nullable File file) {
        SegmentRepository.ioExecutor().execute(() -> {
            if (tripManager != null) tripManager.complete(TripStatus.COMPLETED);
        });
        stateManager.setIdle(file == null ? null : file.getAbsolutePath());
        finishService(false);
    }

    @Override
    public void onFatalError(@NonNull String message) {
        stateManager.setError(message);
        finishService(false);
    }

    @Override
    public void onStorageExhausted() {
        stateManager.setError("Недостаточно свободной памяти");
        notificationManager.showStorageError();
        finishService(false);
    }

    @Override
    public void onLocation(Location location) {
        Location previous = locationFilter.getPrevious();
        boolean valid = locationFilter.isValid(location,
                settingsRepository.getMaxGpsAccuracyMeters());
        double speed = speedCalculator.calculate(location, previous,
                settingsRepository.getParkingSpeedKmh());
        if (speed > 0) lastMovementAt = System.currentTimeMillis();
        metadataSnapshot = new VideoMetadataSnapshot(location.getTime(), speed,
                location.getLatitude(), location.getLongitude(),
                location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE,
                location.hasBearing() ? location.getBearing() : 0);
        segmentManager.onMetadata(metadataSnapshot);
        TripState.Snapshot state = TripState.INSTANCE.live.getValue();
        if (state == null) state = new TripState.Snapshot();
        state.speedKmh = speed;
        state.maxSpeedKmh = Math.max(state.maxSpeedKmh, speed);
        state.gpsActive = valid;
        state.accuracyMeters = location.hasAccuracy() ? location.getAccuracy() : 0;
        TripState.INSTANCE.live.postValue(state);
        boolean accepted = valid;
        SegmentRepository.ioExecutor().execute(() -> {
            if (tripManager == null) return;
            tripManager.recordLocation(location, speed, accepted,
                    settingsRepository.getGpsIntervalMs());
            TripEntity trip = tripManager.getActiveTrip();
            if (trip != null) {
                TripState.Snapshot snapshot = TripState.INSTANCE.live.getValue();
                if (snapshot == null) snapshot = new TripState.Snapshot();
                snapshot.tripId = trip.id;
                snapshot.startedAt = trip.startTime;
                snapshot.distanceMeters = trip.totalDistanceMeters;
                snapshot.maxSpeedKmh = trip.maxSpeedKmh;
                TripState.INSTANCE.live.postValue(snapshot);
            }
        });
        checkAutomaticTripEnd(valid);
    }

    @Override
    public void onGpsStatus(boolean active, String reason) {
        TripState.Snapshot state = TripState.INSTANCE.live.getValue();
        if (state == null) state = new TripState.Snapshot();
        state.gpsActive = active;
        state.gpsMessage = reason;
        TripState.INSTANCE.live.postValue(state);
    }

    @Nullable
    @Override
    public VideoMetadataSnapshot getSnapshot() {
        return metadataSnapshot;
    }

    @Override
    public void onChargingStateChanged(
            @NonNull ChargingStateManager.ChargingState state
    ) {
        if (stateManager == null) {
            return;
        }
        stateManager.setChargingState(state.isCharging(), state.getBatteryPercent());
        charging = state.isCharging();
        if (charging) powerDisconnectedAt = 0;
        else if (powerDisconnectedAt == 0) powerDisconnectedAt = System.currentTimeMillis();
        checkAutomaticTripEnd(metadataSnapshot != null);
        if (sessionStarted && !finishing) {
            notificationManager.update(stateManager.getCurrentState());
        }
    }

    private void checkAutomaticTripEnd(boolean gpsAvailable) {
        if (!sessionStarted || stopping || finishing
                || !segmentManager.isSessionActive()) return;
        long now = System.currentTimeMillis();
        if (tripEndDetector.shouldSuggestEnd(charging,
                metadataSnapshot == null ? 0 : metadataSnapshot.speedKmh,
                lastMovementAt, gpsAvailable, segmentManager.isSessionActive(),
                powerDisconnectedAt, settingsRepository.getPowerEndDelayMs(),
                settingsRepository.getStopEndDelayMs(), now)) {
            requestStop();
        }
    }

    private void handleCameraError(String prefix, Throwable throwable) {
        stateManager.setError(buildErrorMessage(prefix, throwable));
        finishService(false);
    }

    private void finishService(boolean setIdleIfNeeded) {
        if (finishing) {
            return;
        }
        finishing = true;
        cameraRequestId++;
        mainHandler.removeCallbacks(timerRunnable);
        releaseCamera();
        releaseRecordingWakeLock();
        batteryMonitor.stop();
        if (locationTracker != null) locationTracker.stop();
        chargingStateManager.removeListener(this);
        chargingStateManager.resetTripWarningPreference();
        if (setIdleIfNeeded && stateManager.getCurrentState().isServiceActive()) {
            stateManager.setIdle(null);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        notificationManager.cancel();
        stopSelf();
    }

    private void releaseCamera() {
        previewSurfaceProvider = null;
        if (preview != null) {
            preview.setSurfaceProvider(null);
            preview = null;
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        stateManager.setPreviewAttached(false);
    }

    @NonNull
    private String buildErrorMessage(String prefix, Throwable throwable) {
        String details = throwable.getLocalizedMessage();
        return details == null || details.trim().isEmpty()
                ? prefix
                : prefix + ": " + details;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        detachPreview();
        return true;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacks(timerRunnable);
        chargingStateManager.removeListener(this);
        batteryMonitor.stop();
        if (!finishing && segmentManager.isSessionActive()) {
            segmentManager.stop();
        }
        releaseCamera();
        releaseRecordingWakeLock();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        super.onDestroy();
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    public final class LocalBinder extends Binder {
        @NonNull
        public RecordingService getService() {
            return RecordingService.this;
        }
    }
}
