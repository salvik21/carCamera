package com.example.cardvr.storage;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;

public final class StorageManager {
    private final Context context;

    public StorageManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public File getVideoDirectory() {
        File base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (base == null) base = new File(context.getFilesDir(), "videos");
        return new File(base, "CarDvr");
    }

    public long getAvailableBytes() {
        File directory = getVideoDirectory();
        File target = directory.exists() ? directory : directory.getParentFile();
        if (target == null) target = context.getFilesDir();
        return new StatFs(target.getAbsolutePath()).getAvailableBytes();
    }

    public boolean delete(File file) {
        return file != null && (!file.exists() || file.delete());
    }
}
