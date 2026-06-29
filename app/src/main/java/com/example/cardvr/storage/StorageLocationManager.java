package com.example.cardvr.storage;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.cardvr.settings.SettingsRepository;

import java.io.File;

public final class StorageLocationManager {
    public static final String VIDEO_DIRECTORY = "CarDvr";

    private final Context context;
    private final SettingsRepository settings;

    public StorageLocationManager(Context context) {
        this.context = context.getApplicationContext();
        this.settings = new SettingsRepository(this.context);
    }

    public File getSelectedBaseDirectory() {
        File selected = null;
        if (settings.getStorageLocation() == SettingsRepository.STORAGE_LOCATION_SD_CARD) {
            selected = getSdCardBaseDirectory();
        }
        if (selected == null) {
            selected = getPhoneBaseDirectory();
        }
        if (selected == null) {
            selected = new File(context.getFilesDir(), "videos");
        }
        return selected;
    }

    public File getVideoDirectory() {
        return new File(getSelectedBaseDirectory(), VIDEO_DIRECTORY);
    }

    public boolean hasSdCard() {
        return getSdCardBaseDirectory() != null;
    }

    @Nullable
    private File getPhoneBaseDirectory() {
        File[] directories = ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_MOVIES);
        if (directories.length > 0 && directories[0] != null) {
            return directories[0];
        }
        return context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
    }

    @Nullable
    private File getSdCardBaseDirectory() {
        File[] directories = ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_MOVIES);
        for (int i = 1; i < directories.length; i++) {
            File directory = directories[i];
            if (directory != null && Environment.isExternalStorageRemovable(directory)) {
                return directory;
            }
        }
        return null;
    }
}
