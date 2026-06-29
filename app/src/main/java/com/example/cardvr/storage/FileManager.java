package com.example.cardvr.storage;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public final class FileManager {

    private static final String FILE_PREFIX = "DASHCAM_";
    private static final String FILE_EXTENSION = ".mp4";
    private static final String DATE_PATTERN = "yyyy-MM-dd_HH-mm-ss";

    private final StorageLocationManager storageLocationManager;

    public FileManager(Context context) {
        storageLocationManager = new StorageLocationManager(context);
    }

    public File createVideoFile() throws IOException {
        File videoDirectory = storageLocationManager.getVideoDirectory();
        if (!videoDirectory.exists() && !videoDirectory.mkdirs()) {
            throw new IOException("Не удалось создать папку для видео: "
                    + videoDirectory.getAbsolutePath());
        }

        String timestamp = new SimpleDateFormat(DATE_PATTERN, Locale.US).format(new Date());
        String uniqueId = UUID.randomUUID().toString().substring(0, 4);
        File videoFile = new File(videoDirectory,
                FILE_PREFIX + timestamp + "_" + uniqueId + FILE_EXTENSION);
        if (videoFile.exists()) {
            throw new IOException("Файл с таким именем уже существует");
        }
        return videoFile;
    }
}
