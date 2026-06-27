package com.example.cardvr.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cardvr.R;
import com.example.cardvr.camera.CameraController;
import com.example.cardvr.common.PermissionManager;
import com.example.cardvr.power.BatteryMonitor;
import com.example.cardvr.power.ChargingStateManager;
import com.example.cardvr.recording.RecordingStateManager;
import com.example.cardvr.recording.RecordingRecoveryManager;
import com.example.cardvr.recording.SegmentRepository;
import com.example.cardvr.service.RecordingNotificationManager;
import com.example.cardvr.service.RecordingService;
import com.example.cardvr.trips.TripState;
import com.example.cardvr.trips.TripRepository;
import com.example.cardvr.database.TripEntity;
import com.example.cardvr.database.TripStatus;

import java.util.Locale;

public final class MainActivity extends AppCompatActivity implements
        CameraController.Listener,
        ChargingStateManager.Listener {

    private static final String PREFS_NAME = "recorder_ui_settings";
    private static final String PREF_AUTO_OFF_POSITION = "preview_auto_off_position";
    private static final String PREF_LENS_FACING = "selected_lens_facing";

    private PreviewView previewView;
    private View regularControls;
    private View minimalScreen;
    private TextView statusTextView;
    private TextView durationTextView;
    private TextView chargingTextView;
    private TextView speedTextView;
    private TextView gpsTextView;
    private TextView distanceTextView;
    private TextView minimalDurationTextView;
    private TextView minimalChargingTextView;
    private Button startRecordingButton;
    private Button stopRecordingButton;
    private Button permissionSettingsButton;
    private Button hidePreviewButton;

    private CameraController idleCameraController;
    private PreviewController previewController;
    private RecordingStateManager stateManager;
    private ChargingStateManager chargingStateManager;
    private BatteryMonitor batteryMonitor;

    private RecordingService recordingService;
    private boolean serviceBound;
    private boolean serviceConnectionRegistered;
    private boolean cameraStartRequested;
    private boolean permissionRequestInProgress;
    private boolean activityStarted;
    private boolean startRequestPending;
    private boolean chargingStartHandled;
    private boolean tripEndRequested;
    private int selectedLensFacing = CameraSelector.LENS_FACING_BACK;
    private RecordingStateManager.Status lastStatus = RecordingStateManager.Status.IDLE;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> handlePermissionResult()
            );
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> attemptStartRecordingAfterLocation()
            );

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecordingService.LocalBinder localBinder =
                    (RecordingService.LocalBinder) service;
            recordingService = localBinder.getService();
            serviceBound = true;
            if (previewController.isPreviewVisible()) {
                recordingService.attachPreview(previewView.getSurfaceProvider());
            } else {
                recordingService.detachPreview();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            serviceConnectionRegistered = false;
            recordingService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        bindViews();
        configureWindowInsets();
        SegmentRepository recoveryRepository = new SegmentRepository(this);
        SegmentRepository.ioExecutor().execute(
                () -> new RecordingRecoveryManager(recoveryRepository).recover());
        stateManager = RecordingStateManager.getInstance();
        chargingStateManager = ChargingStateManager.getInstance();
        batteryMonitor = new BatteryMonitor(this);
        idleCameraController = new CameraController(this, this, previewView, this);
        previewController = new PreviewController(
                previewView,
                regularControls,
                minimalScreen,
                this::handlePreviewVisibilityChanged
        );
        configureControls();
        stateManager.getStateLiveData().observe(this, this::renderRecordingState);
        TripState.INSTANCE.observe().observe(this, this::renderTripState);
        chargingStateManager.addListener(this);
        requestPermissionsOrPrepareCamera();
        checkActiveTripRecovery();
    }

    private void bindViews() {
        previewView = findViewById(R.id.previewView);
        regularControls = findViewById(R.id.controlsPanel);
        minimalScreen = findViewById(R.id.minimalRecordingScreen);
        statusTextView = findViewById(R.id.statusTextView);
        durationTextView = findViewById(R.id.durationTextView);
        chargingTextView = findViewById(R.id.chargingTextView);
        speedTextView = findViewById(R.id.speedTextView);
        gpsTextView = findViewById(R.id.gpsTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        minimalDurationTextView = findViewById(R.id.minimalDurationTextView);
        minimalChargingTextView = findViewById(R.id.minimalChargingTextView);
        startRecordingButton = findViewById(R.id.startRecordingButton);
        stopRecordingButton = findViewById(R.id.stopRecordingButton);
        permissionSettingsButton = findViewById(R.id.permissionSettingsButton);
        hidePreviewButton = findViewById(R.id.hidePreviewButton);
    }

    private void configureWindowInsets() {
        View rootView = findViewById(R.id.rootView);
        View controlsPanel = findViewById(R.id.controlsPanel);
        View minimalScreen = findViewById(R.id.minimalRecordingScreen);

        int rootPaddingLeft = rootView.getPaddingLeft();
        int rootPaddingTop = rootView.getPaddingTop();
        int rootPaddingRight = rootView.getPaddingRight();
        int rootPaddingBottom = rootView.getPaddingBottom();

        int controlsPaddingLeft = controlsPanel.getPaddingLeft();
        int controlsPaddingTop = controlsPanel.getPaddingTop();
        int controlsPaddingRight = controlsPanel.getPaddingRight();
        int controlsPaddingBottom = controlsPanel.getPaddingBottom();

        int minimalPaddingLeft = minimalScreen.getPaddingLeft();
        int minimalPaddingTop = minimalScreen.getPaddingTop();
        int minimalPaddingRight = minimalScreen.getPaddingRight();
        int minimalPaddingBottom = minimalScreen.getPaddingBottom();

        int additionalBottomPadding = getResources()
                .getDimensionPixelSize(R.dimen.navigation_content_padding);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            Insets navigationBars = insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
            );
            int safeBottom = Math.max(systemBars.bottom, navigationBars.bottom);

            view.setPadding(
                    rootPaddingLeft + systemBars.left,
                    rootPaddingTop + systemBars.top,
                    rootPaddingRight + systemBars.right,
                    rootPaddingBottom
            );
            controlsPanel.setPadding(
                    controlsPaddingLeft,
                    controlsPaddingTop,
                    controlsPaddingRight,
                    controlsPaddingBottom + safeBottom + additionalBottomPadding
            );
            minimalScreen.setPadding(
                    minimalPaddingLeft,
                    minimalPaddingTop,
                    minimalPaddingRight,
                    minimalPaddingBottom + safeBottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    private void configureControls() {
        startRecordingButton.setText(R.string.start_trip);
        stopRecordingButton.setText(R.string.end_trip);
        selectedLensFacing = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(PREF_LENS_FACING, CameraSelector.LENS_FACING_BACK);

        startRecordingButton.setOnClickListener(view -> attemptStartRecording());
        stopRecordingButton.setOnClickListener(view -> requestServiceStop());
        findViewById(R.id.minimalStopRecordingButton)
                .setOnClickListener(view -> requestServiceStop());
        hidePreviewButton.setOnClickListener(view -> previewController.hidePreview());
        findViewById(R.id.showPreviewButton)
                .setOnClickListener(view -> previewController.showPreview());
        permissionSettingsButton.setOnClickListener(view -> openApplicationSettings());
        findViewById(R.id.protectRecordingButton).setOnClickListener(view -> {
            if (stateManager.getCurrentState().isServiceActive()) {
                startService(new Intent(this, RecordingService.class)
                        .setAction(RecordingService.ACTION_PROTECT));
                Toast.makeText(this, R.string.recording_protected, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.recordingsButton).setOnClickListener(view ->
                startActivity(new Intent(this, RecordingsActivity.class)));
        findViewById(R.id.storageSettingsButton).setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class)));

    }

    private void renderTripState(TripState.Snapshot state) {
        speedTextView.setText(getString(R.string.current_speed,
                String.format(Locale.getDefault(), "%.1f", state.speedKmh),
                String.format(Locale.getDefault(), "%.1f", state.maxSpeedKmh)));
        gpsTextView.setText(state.gpsActive
                ? getString(R.string.gps_active, state.accuracyMeters)
                : getString(R.string.gps_unavailable,
                state.gpsMessage == null ? "" : state.gpsMessage));
        distanceTextView.setText(getString(R.string.trip_distance,
                state.distanceMeters / 1000d));
    }

    private void requestPermissionsOrPrepareCamera() {
        if (PermissionManager.hasAllPermissions(this)) {
            updatePermissionSettingsButton();
            prepareIdleCameraIfNeeded();
            return;
        }
        permissionRequestInProgress = true;
        permissionLauncher.launch(PermissionManager.getStartupPermissions());
    }

    private void handlePermissionResult() {
        permissionRequestInProgress = false;
        updatePermissionSettingsButton();
        if (!PermissionManager.hasCameraPermission(this)) {
            releaseIdleCamera();
            setIdleControlsEnabled(false);
            statusTextView.setText(R.string.status_permissions_settings);
            return;
        }
        prepareIdleCameraIfNeeded();
    }

    private void attemptStartRecording() {
        if (!PermissionManager.hasLocationPermission(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.location_permission_title)
                    .setMessage(R.string.location_permission_explanation)
                    .setNegativeButton(R.string.continue_without_gps,
                            (dialog, which) -> attemptStartRecordingAfterLocation())
                    .setPositiveButton(R.string.allow_location, (dialog, which) ->
                            locationPermissionLauncher.launch(new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            }))
                    .show();
            return;
        }
        attemptStartRecordingAfterLocation();
    }

    private void attemptStartRecordingAfterLocation() {
        if (startRequestPending || stateManager.getCurrentState().isServiceActive()) {
            return;
        }
        if (!PermissionManager.hasCameraPermission(this)) {
            statusTextView.setText(R.string.status_camera_permission_required);
            updatePermissionSettingsButton();
            return;
        }

        ChargingStateManager.ChargingState chargingState =
                BatteryMonitor.readCurrentState(this);
        chargingStateManager.updateState(chargingState);
        if (!chargingState.isCharging()
                && !chargingStateManager.isWarningSuppressedForCurrentTrip()) {
            showBatteryWarning();
        } else {
            startRecordingService();
        }
    }

    private void showBatteryWarning() {
        CheckBox doNotShowAgain = new CheckBox(this);
        doNotShowAgain.setText(R.string.battery_warning_do_not_show_again);
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_content_padding);
        doNotShowAgain.setPadding(padding, 0, padding, 0);

        new AlertDialog.Builder(this)
                .setTitle(R.string.battery_warning_title)
                .setMessage(R.string.battery_warning_message)
                .setView(doNotShowAgain)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.continue_recording, (dialog, which) -> {
                    chargingStateManager.setWarningSuppressedForCurrentTrip(
                            doNotShowAgain.isChecked()
                    );
                    startRecordingService();
                })
                .show();
    }

    private void startRecordingService() {
        if (startRequestPending || stateManager.getCurrentState().isServiceActive()) {
            return;
        }
        startRequestPending = true;
        setIdleControlsEnabled(false);
        statusTextView.setText(R.string.status_service_starting);
        releaseIdleCamera();

        Intent intent = new Intent(this, RecordingService.class)
                .setAction(RecordingService.ACTION_START)
                .putExtra(RecordingService.EXTRA_LENS_FACING, selectedLensFacing);
        try {
            ContextCompat.startForegroundService(this, intent);
            bindRecordingService();
        } catch (RuntimeException exception) {
            startRequestPending = false;
            statusTextView.setText(getString(
                    R.string.status_error,
                    exception.getLocalizedMessage()
            ));
            prepareIdleCameraIfNeeded();
        }
    }

    private void requestServiceStop() {
        RecordingStateManager.State state = stateManager.getCurrentState();
        if (!state.isServiceActive()) {
            return;
        }
        stopRecordingButton.setEnabled(false);
        tripEndRequested = true;
        findViewById(R.id.minimalStopRecordingButton).setEnabled(false);
        startService(new Intent(this, RecordingService.class)
                .setAction(RecordingService.ACTION_STOP));
    }

    private void renderRecordingState(RecordingStateManager.State state) {
        boolean active = state.isServiceActive();
        String duration = RecordingNotificationManager.formatDuration(
                state.getElapsedMillis()
        );
        durationTextView.setText(getString(R.string.duration_value, duration));
        minimalDurationTextView.setText(duration);
        updateChargingViews(state.isCharging(), state.getBatteryPercent());

        switch (state.getStatus()) {
            case STARTING:
                startRequestPending = false;
                statusTextView.setText(R.string.status_service_starting);
                setRecordingControls(false, true, false);
                bindRecordingService();
                break;
            case RECORDING:
                startRequestPending = false;
                statusTextView.setText(state.isProtectedEventMarked()
                        ? R.string.status_protected_stub
                        : R.string.status_recording);
                setRecordingControls(false, true, false);
                bindRecordingService();
                if (lastStatus != RecordingStateManager.Status.RECORDING) {
                    previewController.scheduleAutoHide(getSelectedAutoOffMode());
                }
                break;
            case STOPPING:
                statusTextView.setText(R.string.status_stopping);
                setRecordingControls(false, false, false);
                break;
            case ERROR:
                startRequestPending = false;
                previewController.cancelAutoHide();
                previewController.showPreview();
                unbindRecordingService();
                setIdleControlsEnabled(PermissionManager.hasCameraPermission(this));
                statusTextView.setText(getString(R.string.status_error, state.getMessage()));
                prepareIdleCameraIfNeeded();
                break;
            case IDLE:
            default:
                startRequestPending = false;
                previewController.cancelAutoHide();
                previewController.showPreview();
                unbindRecordingService();
                setIdleControlsEnabled(PermissionManager.hasCameraPermission(this));
                if (state.getOutputPath() != null) {
                    statusTextView.setText(getString(
                            R.string.status_saved,
                            state.getOutputPath()
                    ));
                }
                prepareIdleCameraIfNeeded();
                if (tripEndRequested) {
                    tripEndRequested = false;
                    startActivity(new Intent(this, TripSummaryActivity.class));
                }
                break;
        }

        hidePreviewButton.setVisibility(active ? View.VISIBLE : View.GONE);
        lastStatus = state.getStatus();
    }

    private void checkActiveTripRecovery() {
        SegmentRepository.ioExecutor().execute(() -> {
            TripRepository repository = new TripRepository(this);
            TripEntity active = repository.getActive();
            if (active == null || stateManager.getCurrentState().isServiceActive()) return;
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle(R.string.active_trip_found)
                    .setMessage(R.string.active_trip_recovery)
                    .setNegativeButton(R.string.finish_trip, (dialog, which) ->
                            SegmentRepository.ioExecutor().execute(() ->
                                    repository.finish(active.id, TripStatus.COMPLETED,
                                            System.currentTimeMillis())))
                    .setPositiveButton(R.string.continue_trip,
                            (dialog, which) -> attemptStartRecording())
                    .show());
        });
    }

    private void setRecordingControls(
            boolean startEnabled,
            boolean stopEnabled,
            boolean cameraSelectionEnabled
    ) {
        startRecordingButton.setEnabled(startEnabled);
        stopRecordingButton.setEnabled(stopEnabled);
        findViewById(R.id.minimalStopRecordingButton).setEnabled(stopEnabled);
    }

    private void setIdleControlsEnabled(boolean enabled) {
        setRecordingControls(enabled, false, enabled);
    }

    private void prepareIdleCameraIfNeeded() {
        if (!activityStarted
                || stateManager.getCurrentState().isServiceActive()
                || !PermissionManager.hasCameraPermission(this)
                || !previewController.isPreviewVisible()) {
            return;
        }
        prepareIdleCamera();
    }

    private void prepareIdleCamera() {
        cameraStartRequested = true;
        idleCameraController.startCamera(selectedLensFacing);
    }

    private void releaseIdleCamera() {
        cameraStartRequested = false;
        idleCameraController.release();
    }

    @Override
    public void onCameraReady(@NonNull VideoCapture<Recorder> videoCapture) {
        cameraStartRequested = false;
        if (stateManager.getCurrentState().isServiceActive()) {
            releaseIdleCamera();
            return;
        }
        setIdleControlsEnabled(true);
        statusTextView.setText(PermissionManager.hasMicrophonePermission(this)
                ? R.string.status_camera_ready
                : R.string.status_microphone_unavailable);
    }

    @Override
    public void onCameraError(@NonNull String message) {
        cameraStartRequested = false;
        if (!stateManager.getCurrentState().isServiceActive()) {
            setIdleControlsEnabled(false);
            statusTextView.setText(getString(R.string.status_error, message));
        }
    }

    private void bindRecordingService() {
        if (!activityStarted
                || serviceConnectionRegistered
                || !stateManager.getCurrentState().isServiceActive()) {
            return;
        }
        Intent intent = new Intent(this, RecordingService.class);
        serviceConnectionRegistered = bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    private void unbindRecordingService() {
        if (serviceBound && recordingService != null) {
            recordingService.detachPreview();
        }
        if (serviceConnectionRegistered) {
            unbindService(serviceConnection);
        }
        serviceConnectionRegistered = false;
        serviceBound = false;
        recordingService = null;
    }

    private void handlePreviewVisibilityChanged(boolean visible) {
        if (stateManager.getCurrentState().isServiceActive()) {
            if (serviceBound && recordingService != null) {
                if (visible) {
                    recordingService.attachPreview(previewView.getSurfaceProvider());
                } else {
                    recordingService.detachPreview();
                }
            }
        } else if (visible) {
            prepareIdleCameraIfNeeded();
        } else {
            releaseIdleCamera();
        }
    }

    private PreviewController.AutoOffMode getSelectedAutoOffMode() {
        int position = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(PREF_AUTO_OFF_POSITION, 4);
        switch (position) {
            case 0:
                return PreviewController.AutoOffMode.FIFTEEN_SECONDS;
            case 1:
                return PreviewController.AutoOffMode.THIRTY_SECONDS;
            case 2:
                return PreviewController.AutoOffMode.ONE_MINUTE;
            case 3:
                return PreviewController.AutoOffMode.IMMEDIATELY;
            case 4:
            default:
                return PreviewController.AutoOffMode.NEVER;
        }
    }

    private void updatePermissionSettingsButton() {
        boolean missing = !PermissionManager.hasCameraPermission(this)
                || !PermissionManager.hasMicrophonePermission(this)
                || !PermissionManager.hasNotificationPermission(this);
        permissionSettingsButton.setVisibility(missing ? View.VISIBLE : View.GONE);
    }

    private void openApplicationSettings() {
        startActivity(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null)
        ));
    }

    @Override
    public void onChargingStateChanged(
            @NonNull ChargingStateManager.ChargingState state
    ) {
        runOnUiThread(() -> {
            updateChargingViews(state.isCharging(), state.getBatteryPercent());
            if (!state.isCharging() || chargingStartHandled
                    || stateManager.getCurrentState().isServiceActive()) return;
            chargingStartHandled = true;
            int mode = new com.example.cardvr.settings.SettingsRepository(this)
                    .getTripStartMode();
            if (mode == 1) {
                attemptStartRecording();
            } else if (mode == 0) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.charging_trip_prompt_title)
                        .setMessage(R.string.charging_trip_prompt)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.start_trip,
                                (dialog, which) -> attemptStartRecording())
                        .show();
            }
        });
    }

    private void updateChargingViews(boolean charging, int batteryPercent) {
        String power = getString(charging
                ? R.string.charging_connected
                : R.string.charging_disconnected);
        String battery = batteryPercent >= 0
                ? getString(R.string.battery_percent, batteryPercent)
                : getString(R.string.battery_unknown);
        String fullText = getString(R.string.charging_and_battery, power, battery);
        chargingTextView.setText(fullText);
        minimalChargingTextView.setText(fullText);
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityStarted = true;
        batteryMonitor.start();
        if (stateManager.getCurrentState().isServiceActive()) {
            releaseIdleCamera();
            bindRecordingService();
        } else {
            prepareIdleCameraIfNeeded();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int configuredLens = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(PREF_LENS_FACING, CameraSelector.LENS_FACING_BACK);
        if (configuredLens != selectedLensFacing
                && !stateManager.getCurrentState().isServiceActive()) {
            selectedLensFacing = configuredLens;
            releaseIdleCamera();
        }
        if (permissionRequestInProgress) {
            return;
        }
        updatePermissionSettingsButton();
        if (!PermissionManager.hasCameraPermission(this)) {
            releaseIdleCamera();
            setIdleControlsEnabled(false);
            statusTextView.setText(R.string.status_camera_permission_required);
        } else if (!stateManager.getCurrentState().isServiceActive()) {
            prepareIdleCameraIfNeeded();
        }
    }

    @Override
    protected void onStop() {
        activityStarted = false;
        batteryMonitor.stop();
        if (stateManager.getCurrentState().isServiceActive()) {
            if (serviceBound && recordingService != null) {
                recordingService.detachPreview();
            }
            unbindRecordingService();
        } else {
            releaseIdleCamera();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        previewController.release();
        chargingStateManager.removeListener(this);
        unbindRecordingService();
        releaseIdleCamera();
        super.onDestroy();
    }
}
