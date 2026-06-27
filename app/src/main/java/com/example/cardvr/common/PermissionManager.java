package com.example.cardvr.common;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public final class PermissionManager {

    private PermissionManager() {
    }

    public static boolean hasCameraPermission(Context context) {
        return hasPermission(context, Manifest.permission.CAMERA);
    }

    public static boolean hasMicrophonePermission(Context context) {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO);
    }

    public static boolean hasAllPermissions(Context context) {
        return hasCameraPermission(context)
                && hasMicrophonePermission(context)
                && hasNotificationPermission(context);
    }

    public static boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || hasPermission(context, Manifest.permission.POST_NOTIFICATIONS);
    }

    public static boolean hasLocationPermission(Context context) {
        return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                || hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static String[] getStartupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }
        return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };
    }

    private static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}
