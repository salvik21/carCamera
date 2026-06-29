package com.example.cardvr.cloud;

import com.example.cardvr.database.CloudDestination;
import com.example.cardvr.database.CloudUploadTaskEntity;

public final class CloudStorageManager {
    private final GoogleDriveManager googleDrive;

    public CloudStorageManager(GoogleDriveManager googleDrive) {
        this.googleDrive = googleDrive;
    }

    public GoogleDriveManager.UploadResult upload(CloudUploadTaskEntity task,
                                                  String checksum,
                                                  String folderId) throws Exception {
        if (task.destination == CloudDestination.GOOGLE_DRIVE) {
            return googleDrive.upload(task, checksum, folderId);
        }
        throw new UnsupportedOperationException("Cloud destination is not supported yet");
    }
}
