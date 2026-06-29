package com.example.cardvr.cloud;

import com.example.cardvr.database.CloudDestination;
import com.example.cardvr.database.CloudFolderEntity;

public final class CloudFolderManager {
    private final CloudUploadRepository repository;

    public CloudFolderManager(android.content.Context context) {
        repository = new CloudUploadRepository(context);
    }

    public String folderPathForTask(com.example.cardvr.database.CloudUploadTaskEntity task) {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date());
        switch (task.type) {
            case EVENT_JSON:
            case CRITICAL_EVENT_VIDEO:
                return "Dashcam/Events/" + date;
            case PROTECTED_VIDEO:
                return "Dashcam/Protected/" + date;
            case TRIP_SEGMENT:
            case TRIP_METADATA:
                return "Dashcam/Trips";
            case MANUAL_UPLOAD:
            default:
                return "Dashcam";
        }
    }

    public String getCachedFolderId(String path) {
        CloudFolderEntity folder = repository.getFolder(path);
        return folder == null ? null : folder.remoteFolderId;
    }

    public void cacheFolderId(String path, String remoteFolderId) {
        long now = System.currentTimeMillis();
        CloudFolderEntity folder = new CloudFolderEntity();
        folder.provider = CloudDestination.GOOGLE_DRIVE;
        folder.path = path;
        folder.remoteFolderId = remoteFolderId;
        folder.createdAt = now;
        folder.updatedAt = now;
        repository.saveFolder(folder);
    }
}
