package com.example.cardvr.sensors;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class SensorCalibrationManager {
    private static final String PREFS = "sensor_calibration";
    private static final String DONE = "done";
    private static final String FORWARD_X = "forward_x";
    private static final String FORWARD_Y = "forward_y";
    private static final String FORWARD_Z = "forward_z";
    private static final String UP_X = "up_x";
    private static final String UP_Y = "up_y";
    private static final String UP_Z = "up_z";
    private final SharedPreferences preferences;

    public SensorCalibrationManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isCalibrated() {
        return preferences.getBoolean(DONE, false);
    }

    public void calibrateFromStillSample(@NonNull SensorSample sample) {
        double ax = sample.accelerationX;
        double ay = sample.accelerationY;
        double az = sample.accelerationZ;
        double norm = Math.sqrt(ax * ax + ay * ay + az * az);
        if (norm < 1.0) norm = 9.81;
        double upX = -ax / norm;
        double upY = -ay / norm;
        double upZ = -az / norm;
        double forwardX = 0;
        double forwardY = Math.abs(upY) > 0.8 ? 1 : 0;
        double forwardZ = Math.abs(upY) > 0.8 ? 0 : -1;
        preferences.edit()
                .putBoolean(DONE, true)
                .putFloat((FORWARD_X), (float) forwardX)
                .putFloat((FORWARD_Y), (float) forwardY)
                .putFloat((FORWARD_Z), (float) forwardZ)
                .putFloat((UP_X), (float) upX)
                .putFloat((UP_Y), (float) upY)
                .putFloat((UP_Z), (float) upZ)
                .apply();
    }

    @NonNull
    public ProjectedAcceleration project(double x, double y, double z) {
        double fx = preferences.getFloat(FORWARD_X, 0);
        double fy = preferences.getFloat(FORWARD_Y, 1);
        double fz = preferences.getFloat(FORWARD_Z, 0);
        double ux = preferences.getFloat(UP_X, 0);
        double uy = preferences.getFloat(UP_Y, 0);
        double uz = preferences.getFloat(UP_Z, 1);
        double longitudinal = dot(x, y, z, fx, fy, fz);
        double vertical = dot(x, y, z, ux, uy, uz);
        double totalSq = x * x + y * y + z * z;
        double lateralSq = Math.max(0, totalSq - longitudinal * longitudinal - vertical * vertical);
        double lateral = Math.sqrt(lateralSq);
        return new ProjectedAcceleration(longitudinal, lateral, vertical);
    }

    private static double dot(double x, double y, double z, double ax, double ay, double az) {
        return x * ax + y * ay + z * az;
    }

    public static final class ProjectedAcceleration {
        public final double longitudinal;
        public final double lateral;
        public final double vertical;

        ProjectedAcceleration(double longitudinal, double lateral, double vertical) {
            this.longitudinal = longitudinal;
            this.lateral = lateral;
            this.vertical = vertical;
        }
    }
}
