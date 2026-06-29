package com.example.cardvr.diagnostics;

import android.content.Context;
import android.os.Build;

import com.example.cardvr.cloud.CloudUploadRepository;
import com.example.cardvr.database.ErrorLogEntity;
import com.example.cardvr.database.VideoSegmentEntity;
import com.example.cardvr.recording.SegmentRepository;
import com.example.cardvr.storage.StorageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class DiagnosticReportManager {
    private final Context context;

    public DiagnosticReportManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public File exportTextReport() throws Exception {
        File dir = new File(context.getFilesDir(), "diagnostics");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("No diagnostics dir");
        File file = new File(dir, "diagnostic_" + System.currentTimeMillis() + ".txt");
        String report = buildReport();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(report.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    public String buildReport() {
        StringBuilder b = new StringBuilder();
        StorageManager storage = new StorageManager(context);
        b.append("CarCamera diagnostic report\n");
        b.append("Created: ").append(DateFormat.getDateTimeInstance().format(new Date())).append('\n');
        b.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        b.append("Android: ").append(Build.VERSION.RELEASE).append(" API ").append(Build.VERSION.SDK_INT).append('\n');
        b.append("Available storage: ").append(storage.getAvailableBytes()).append('\n');
        b.append("Cloud active tasks: ").append(new CloudUploadRepository(context).countActiveTasks()).append('\n');
        b.append("\nRecent errors:\n");
        List<ErrorLogEntity> errors = new ErrorLogRepository(context).getRecent(30);
        for (ErrorLogEntity e : errors) {
            b.append(e.timestamp).append(' ').append(e.severity).append(' ')
                    .append(e.module).append('/').append(e.errorType).append(": ")
                    .append(e.message).append('\n');
        }
        b.append("\nRecent segments:\n");
        List<VideoSegmentEntity> segments = new SegmentRepository(context).getAll();
        int count = Math.min(20, segments.size());
        for (int i = 0; i < count; i++) {
            VideoSegmentEntity s = segments.get(i);
            b.append(s.id).append(' ').append(s.status).append(' ')
                    .append(s.fileName).append(' ').append(s.sizeBytes).append('\n');
        }
        return b.toString();
    }
}
