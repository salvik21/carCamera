package com.example.cardvr.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

public final class SensorController implements SensorEventListener {
    public interface Listener {
        void onSensorAvailability(boolean accelerometer, boolean linearAcceleration,
                                  boolean gyroscope, boolean rotationVector);
        void onSensorSample(@NonNull SensorSample sample);
    }

    private final SensorManager sensorManager;
    private final SensorDataFilter filter;
    private final SensorCalibrationManager calibration;
    private final SensorSnapshotProvider snapshots;
    private final Listener listener;
    private HandlerThread thread;
    private Handler handler;
    private String orientation = "unknown";
    private boolean running;

    public SensorController(@NonNull Context context,
                            @NonNull SensorDataFilter filter,
                            @NonNull SensorCalibrationManager calibration,
                            @NonNull SensorSnapshotProvider snapshots,
                            @NonNull Listener listener) {
        this.sensorManager = (SensorManager) context.getApplicationContext()
                .getSystemService(Context.SENSOR_SERVICE);
        this.filter = filter;
        this.calibration = calibration;
        this.snapshots = snapshots;
        this.listener = listener;
    }

    public void start(@NonNull DetectionSettings settings) {
        if (running) return;
        running = true;
        thread = new HandlerThread("dvr-sensors");
        thread.start();
        handler = new Handler(thread.getLooper());
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        listener.onSensorAvailability(accel != null, linear != null, gyro != null, rotation != null);
        int delay = settings.sensorDelayMicros();
        if (accel != null) sensorManager.registerListener(this, accel, delay, handler);
        if (linear != null) sensorManager.registerListener(this, linear, delay, handler);
        if (gyro != null) sensorManager.registerListener(this, gyro, delay, handler);
        if (rotation != null) sensorManager.registerListener(this, rotation, delay, handler);
    }

    public void stop() {
        if (!running) return;
        running = false;
        sensorManager.unregisterListener(this);
        if (thread != null) {
            thread.quitSafely();
            thread = null;
            handler = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!running) return;
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ACCELEROMETER) {
            filter.onAccelerometer(event.values, event.timestamp);
        } else if (type == Sensor.TYPE_LINEAR_ACCELERATION) {
            filter.onLinearAcceleration(event.values);
        } else if (type == Sensor.TYPE_GYROSCOPE) {
            filter.onGyroscope(event.values);
        } else if (type == Sensor.TYPE_ROTATION_VECTOR) {
            orientation = orientationFromRotation(event.values);
        }
        SensorSample sample = filter.snapshot(calibration, orientation);
        if (sample != null) {
            snapshots.update(sample);
            listener.onSensorSample(sample);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private static String orientationFromRotation(float[] values) {
        if (values.length < 3) return "rotation_vector";
        return String.format(java.util.Locale.US, "x=%.2f,y=%.2f,z=%.2f",
                values[0], values[1], values[2]);
    }
}
