package com.example.cardvr.diagnostics;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.cardvr.database.AppDatabase;
import com.example.cardvr.database.ErrorLogDao;
import com.example.cardvr.database.ErrorLogEntity;
import com.example.cardvr.database.ErrorSeverity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public final class ErrorLogRepository {
    private static final int MAX_LOGS = 500;
    private final ErrorLogDao dao;
    private final Context context;

    public ErrorLogRepository(Context context) {
        this.context = context.getApplicationContext();
        dao = AppDatabase.getInstance(context).errorLogDao();
    }

    public long log(String module, String type, ErrorSeverity severity, String message,
                    Throwable throwable, Long tripId, Long segmentId, Long eventId,
                    boolean recovered, boolean userVisible, boolean simulated) {
        ErrorLogEntity entity = new ErrorLogEntity();
        entity.timestamp = System.currentTimeMillis();
        entity.module = safe(module);
        entity.errorType = safe(type);
        entity.severity = severity == null ? ErrorSeverity.ERROR : severity;
        entity.message = sanitize(message);
        entity.stackTrace = throwable == null ? null : sanitize(stackTrace(throwable));
        entity.tripId = tripId;
        entity.segmentId = segmentId;
        entity.eventId = eventId;
        entity.recovered = recovered;
        entity.userVisible = userVisible;
        entity.simulated = simulated;
        entity.appVersion = "1.0";
        entity.androidVersion = Build.VERSION.RELEASE;
        entity.deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        long id = dao.insert(entity);
        dao.trim(MAX_LOGS);
        Log.println(toPriority(entity.severity), "CarDvr/" + entity.module,
                entity.errorType + ": " + entity.message);
        return id;
    }

    public LiveData<List<ErrorLogEntity>> observeRecent() {
        return dao.observeRecent(100);
    }

    public List<ErrorLogEntity> getRecent(int limit) {
        return dao.getRecent(limit);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        return value
                .replaceAll("(?i)access_token=[^\\s&]+", "access_token=<redacted>")
                .replaceAll("(?i)refresh_token=[^\\s&]+", "refresh_token=<redacted>")
                .replaceAll("(?i)password=[^\\s&]+", "password=<redacted>");
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static int toPriority(ErrorSeverity severity) {
        switch (severity) {
            case INFO: return Log.INFO;
            case WARNING: return Log.WARN;
            case CRITICAL:
            case ERROR:
            default: return Log.ERROR;
        }
    }
}
