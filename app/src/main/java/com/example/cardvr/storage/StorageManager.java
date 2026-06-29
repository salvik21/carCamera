package com.example.cardvr.storage;

import android.content.Context;
import android.os.StatFs;

import java.io.File;

public final class StorageManager {
    private final Context appContext;
    private final StorageLocationManager storageLocationManager;

    public StorageManager(Context context) {
        appContext = context.getApplicationContext();
        storageLocationManager = new StorageLocationManager(appContext);
    }

    public File getVideoDirectory() {
        return storageLocationManager.getVideoDirectory();
    }

    public long getAvailableBytes() {
        File directory = getVideoDirectory();
        File target = directory.exists() ? directory : directory.getParentFile();
        if (target == null) target = appContext.getFilesDir();
        return new StatFs(target.getAbsolutePath()).getAvailableBytes();
    }

    public boolean delete(File file) {
        return file != null && (!file.exists() || file.delete());
    }
}
