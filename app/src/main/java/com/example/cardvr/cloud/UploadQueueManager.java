package com.example.cardvr.cloud;

import android.content.Context;

import com.example.cardvr.database.CloudDestination;
import com.example.cardvr.database.CloudUploadStatus;
import com.example.cardvr.database.CloudUploadTaskEntity;
import com.example.cardvr.database.CloudUploadType;
import com.example.cardvr.database.EventEntity;
import com.example.cardvr.database.EventSeverity;
import com.example.cardvr.database.EventType;
import com.example.cardvr.database.VideoSegmentEntity;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

public final class UploadQueueManager {
    private final Context context;
    private final CloudUploadRepository repository;
    private final CloudSettingsRepository settings;
    private final UploadScheduler scheduler;

    public UploadQueueManager(Context context) {
        this.context = context.getApplicationContext();
        repository = new CloudUploadRepository(context);
        settings = new CloudSettingsRepository(context);
        scheduler = new UploadScheduler(context);
    }

    public void enqueueSegment(VideoSegmentEntity segment, CloudUploadType type) {
        if (segment.filePath == null || segment.filePath.trim().isEmpty()) return;
        File file = new File(segment.filePath);
        CloudUploadTaskEntity task = baseTask(file, type, priority(type));
        task.localFileId = segment.id;
        task.requiresWifi = type == CloudUploadType.TRIP_SEGMENT;
        task.allowMobileData = type == CloudUploadType.EVENT_JSON
                || type == CloudUploadType.CRITICAL_EVENT_VIDEO
                || settings.networkMode() == CloudSettingsRepository.NetworkMode.WIFI_AND_MOBILE;
        long id = repository.insertTask(task);
        if (id > 0) repository.markSegmentQueued(segment.id);
        scheduler.schedule();
    }

    public void enqueueEventJson(EventEntity event, int batteryPercent, boolean charging) {
        try {
            File dir = new File(context.getFilesDir(), "cloud-events");
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            File file = new File(dir, "event_" + event.id + ".json");
            JSONObject json = new JSONObject();
            json.put("eventId", event.id);
            json.put("tripId", event.tripId);
            json.put("eventType", event.type.name());
            json.put("severity", event.severity.name());
            json.put("confidence", event.confidence);
            json.put("timestamp", event.timestamp);
            json.put("latitude", event.latitude);
            json.put("longitude", event.longitude);
            json.put("speedBeforeKmh", event.speedBeforeKmh);
            json.put("speedAfterKmh", event.speedAfterKmh);
            json.put("impactG", event.impactG);
            json.put("batteryPercent", batteryPercent);
            json.put("charging", charging);
            json.put("gpsAccuracyMeters", event.gpsAccuracyMeters);
            json.put("protectedFromTime", event.protectedFromTime);
            json.put("protectedUntilTime", event.protectedUntilTime);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(json.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            CloudUploadTaskEntity task = baseTask(file, CloudUploadType.EVENT_JSON, 1);
            task.allowMobileData = true;
            repository.insertTask(task);
            scheduler.schedule();
        } catch (Exception e) {
            repository.logError(null, "EVENT_JSON", e.getMessage(), false);
        }
    }

    public boolean shouldAutoUpload(EventEntity event) {
        CloudSettingsRepository.AutoUploadMode mode = settings.autoUploadMode();
        if (mode == CloudSettingsRepository.AutoUploadMode.NOTHING) return false;
        if (event.type == EventType.POSSIBLE_CRASH) return true;
        if (event.severity == EventSeverity.CRITICAL || event.severity == EventSeverity.HIGH) {
            return mode == CloudSettingsRepository.AutoUploadMode.CRASH_AND_IMPACT
                    || mode == CloudSettingsRepository.AutoUploadMode.ALL_PROTECTED;
        }
        return mode == CloudSettingsRepository.AutoUploadMode.ALL_PROTECTED;
    }

    public void createSimulatedJsonTask() {
        EventEntity event = new EventEntity();
        event.id = System.currentTimeMillis();
        event.type = EventType.POSSIBLE_CRASH;
        event.severity = EventSeverity.CRITICAL;
        event.confidence = 92;
        event.timestamp = System.currentTimeMillis();
        event.impactG = 2.8;
        enqueueEventJson(event, 50, true);
    }

    public void clearSimulatedQueue() {
        repository.clearSimulatedTasks();
    }

    private static CloudUploadTaskEntity baseTask(File file, CloudUploadType type, int priority) {
        long now = System.currentTimeMillis();
        CloudUploadTaskEntity task = new CloudUploadTaskEntity();
        task.localPath = file.getAbsolutePath();
        task.destination = CloudDestination.GOOGLE_DRIVE;
        task.type = type;
        task.priority = priority;
        task.status = CloudUploadStatus.PENDING;
        task.sizeBytes = file.isFile() ? file.length() : 0L;
        task.createdAt = now;
        task.updatedAt = now;
        return task;
    }

    private static int priority(CloudUploadType type) {
        switch (type) {
            case EVENT_JSON: return 1;
            case CRITICAL_EVENT_VIDEO: return 2;
            case PROTECTED_VIDEO: return 3;
            case TRIP_SEGMENT: return 4;
            case MANUAL_UPLOAD:
            case TRIP_METADATA:
            default: return 5;
        }
    }
}
