package com.example.cardvr.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "cloud_upload_tasks", indices = {
        @Index("localFileId"),
        @Index("status"),
        @Index(value = {"localPath", "destination", "type"}, unique = true)
})
public class CloudUploadTaskEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    @Nullable public Long localFileId;
    @Nullable public String localPath;
    @Nullable public String contentUri;
    @NonNull public CloudDestination destination = CloudDestination.GOOGLE_DRIVE;
    @Nullable public String remoteFolderId;
    @Nullable public String remoteFileId;
    @NonNull public CloudUploadType type = CloudUploadType.MANUAL_UPLOAD;
    public int priority;
    @NonNull public CloudUploadStatus status = CloudUploadStatus.PENDING;
    public long sizeBytes;
    public long uploadedBytes;
    public int retryCount;
    @Nullable public String lastError;
    public boolean requiresWifi;
    public boolean allowMobileData;
    @Nullable public String checksum;
    public boolean checksumVerified;
    public boolean simulated;
    public long createdAt;
    public long updatedAt;
    @Nullable public Long completedAt;
}
