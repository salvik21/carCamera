package com.example.cardvr.cloud;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cardvr.database.CloudUploadStatus;
import com.example.cardvr.database.CloudUploadTaskEntity;

import java.io.File;

public final class CloudUploadWorker extends Worker {
    public CloudUploadWorker(@NonNull Context context,
                             @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        CloudUploadRepository repository = new CloudUploadRepository(context);
        CloudAuthenticationManager auth = new CloudAuthenticationManager(context);
        if (!auth.isConnected()) {
            CloudUploadTaskEntity task = repository.getNextRunnableTask();
            if (task != null) repository.setTaskStatus(task.id,
                    CloudUploadStatus.WAITING_FOR_AUTH, "Google Drive не подключён");
            return Result.success();
        }
        CloudUploadTaskEntity task = repository.getNextRunnableTask();
        if (task == null) return Result.success();
        NetworkPolicyManager.Decision decision = new NetworkPolicyManager(context).canUpload(task);
        if (decision != NetworkPolicyManager.Decision.ALLOW) {
            CloudUploadStatus status = decision == NetworkPolicyManager.Decision.WAIT_WIFI
                    ? CloudUploadStatus.WAITING_FOR_WIFI : CloudUploadStatus.WAITING_FOR_NETWORK;
            repository.setTaskStatus(task.id, status, decision.name());
            return Result.retry();
        }
        try {
            File file = task.localPath == null ? null : new File(task.localPath);
            if (file == null || !file.isFile()) {
                repository.setTaskStatus(task.id, CloudUploadStatus.FAILED,
                        "Локальный файл отсутствует");
                return Result.success();
            }
            repository.setTaskStatus(task.id, CloudUploadStatus.UPLOADING, null);
            String checksum = new ChecksumManager().sha256(file);
            CloudFolderManager folders = new CloudFolderManager(context);
            String folderId = folders.getCachedFolderId(folders.folderPathForTask(task));
            GoogleDriveManager.UploadResult upload = new CloudStorageManager(
                    new GoogleDriveManager(auth)).upload(task, checksum, folderId);
            task.remoteFileId = upload.remoteFileId;
            task.checksum = checksum;
            task.checksumVerified = upload.remoteFileId != null && !upload.remoteFileId.isEmpty();
            task.uploadedBytes = task.sizeBytes;
            task.status = CloudUploadStatus.COMPLETED;
            task.completedAt = System.currentTimeMillis();
            task.updatedAt = task.completedAt;
            repository.updateTask(task);
            repository.markSegmentCompleted(task.localFileId, upload.remoteFileId, checksum);
            new TrafficUsageManager(context).recordUploaded(task.sizeBytes);
            return repository.getNextRunnableTask() == null ? Result.success() : Result.retry();
        } catch (SecurityException e) {
            repository.setTaskStatus(task.id, CloudUploadStatus.WAITING_FOR_AUTH, e.getMessage());
            repository.logError(task.id, "AUTH", e.getMessage(), true);
            return Result.success();
        } catch (Exception e) {
            task.retryCount++;
            task.lastError = e.getMessage();
            task.status = task.retryCount >= 5 ? CloudUploadStatus.FAILED : CloudUploadStatus.PENDING;
            task.updatedAt = System.currentTimeMillis();
            repository.updateTask(task);
            repository.logError(task.id, "UPLOAD", e.getMessage(), task.retryCount < 5);
            return task.retryCount >= 5 ? Result.success() : Result.retry();
        }
    }
}
