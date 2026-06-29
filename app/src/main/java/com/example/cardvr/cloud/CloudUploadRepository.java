package com.example.cardvr.cloud;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cardvr.database.AppDatabase;
import com.example.cardvr.database.CloudAccountEntity;
import com.example.cardvr.database.CloudDestination;
import com.example.cardvr.database.CloudErrorEntity;
import com.example.cardvr.database.CloudFolderEntity;
import com.example.cardvr.database.CloudStatus;
import com.example.cardvr.database.CloudUploadDao;
import com.example.cardvr.database.CloudUploadStatus;
import com.example.cardvr.database.CloudUploadTaskEntity;
import com.example.cardvr.database.TrafficUsageEntity;
import com.example.cardvr.database.VideoSegmentDao;

import java.util.List;

public final class CloudUploadRepository {
    private final CloudUploadDao dao;
    private final VideoSegmentDao segments;

    public CloudUploadRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        dao = db.cloudUploadDao();
        segments = db.videoSegmentDao();
    }

    public long insertTask(CloudUploadTaskEntity task) { return dao.insertTask(task); }
    public void updateTask(CloudUploadTaskEntity task) { dao.updateTask(task); }
    public LiveData<List<CloudUploadTaskEntity>> observeTasks() { return dao.observeTasks(); }
    public List<CloudUploadTaskEntity> getTasks() { return dao.getTasks(); }
    public CloudUploadTaskEntity getNextRunnableTask() { return dao.getNextRunnableTask(); }
    public int countActiveTasks() { return dao.countActiveTasks(); }
    public int countWaitingForWifi() { return dao.countWaitingForWifi(); }
    public CloudAccountEntity getAccount() { return dao.getAccount(CloudDestination.GOOGLE_DRIVE); }
    public long saveAccount(CloudAccountEntity account) { return dao.insertAccount(account); }
    public void updateAccount(CloudAccountEntity account) { dao.updateAccount(account); }
    public CloudFolderEntity getFolder(String path) { return dao.getFolder(CloudDestination.GOOGLE_DRIVE, path); }
    public long saveFolder(CloudFolderEntity folder) { return dao.insertFolder(folder); }
    public TrafficUsageEntity getTraffic(int yearMonth) { return dao.getTraffic(yearMonth); }
    public void saveTraffic(TrafficUsageEntity usage) { dao.insertTraffic(usage); }
    public void logError(Long taskId, String category, String message, boolean retryable) {
        CloudErrorEntity error = new CloudErrorEntity();
        error.taskId = taskId;
        error.category = category;
        error.message = message == null ? "" : message;
        error.retryable = retryable;
        error.createdAt = System.currentTimeMillis();
        dao.insertError(error);
    }
    public void setTaskStatus(long id, CloudUploadStatus status, String error) {
        dao.setTaskStatus(id, status, System.currentTimeMillis(), error);
    }
    public void updateProgress(long id, long bytes) {
        dao.updateProgress(id, bytes, System.currentTimeMillis());
    }
    public void markSegmentQueued(Long id) {
        if (id != null) segments.setCloudStatus(id, CloudStatus.QUEUED);
    }
    public void markSegmentCompleted(Long id, String remoteFileId, String checksum) {
        if (id != null) {
            segments.updateCloudResult(id, CloudStatus.COMPLETED, remoteFileId,
                    checksum, System.currentTimeMillis());
        }
    }
    public void clearSimulatedTasks() { dao.clearSimulatedTasks(); }
}
