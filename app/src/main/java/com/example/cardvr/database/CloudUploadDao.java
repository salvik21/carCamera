package com.example.cardvr.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CloudUploadDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertTask(CloudUploadTaskEntity task);

    @Update
    void updateTask(CloudUploadTaskEntity task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertAccount(CloudAccountEntity account);

    @Update
    void updateAccount(CloudAccountEntity account);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertFolder(CloudFolderEntity folder);

    @Insert
    long insertError(CloudErrorEntity error);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTraffic(TrafficUsageEntity usage);

    @Query("SELECT * FROM cloud_upload_tasks ORDER BY status ASC, priority ASC, createdAt ASC")
    LiveData<List<CloudUploadTaskEntity>> observeTasks();

    @Query("SELECT * FROM cloud_upload_tasks ORDER BY status ASC, priority ASC, createdAt ASC")
    List<CloudUploadTaskEntity> getTasks();

    @Query("SELECT * FROM cloud_upload_tasks WHERE status IN ('PENDING','WAITING_FOR_NETWORK','WAITING_FOR_WIFI') ORDER BY priority ASC, createdAt ASC LIMIT 1")
    CloudUploadTaskEntity getNextRunnableTask();

    @Query("SELECT COUNT(*) FROM cloud_upload_tasks WHERE status NOT IN ('COMPLETED','FAILED','CANCELLED')")
    int countActiveTasks();

    @Query("SELECT COUNT(*) FROM cloud_upload_tasks WHERE status='WAITING_FOR_WIFI'")
    int countWaitingForWifi();

    @Query("SELECT * FROM cloud_accounts WHERE provider=:provider LIMIT 1")
    CloudAccountEntity getAccount(CloudDestination provider);

    @Query("SELECT * FROM cloud_folders WHERE provider=:provider AND path=:path LIMIT 1")
    CloudFolderEntity getFolder(CloudDestination provider, String path);

    @Query("SELECT * FROM traffic_usage WHERE yearMonth=:yearMonth LIMIT 1")
    TrafficUsageEntity getTraffic(int yearMonth);

    @Query("UPDATE cloud_upload_tasks SET status=:status, updatedAt=:updatedAt, lastError=:lastError WHERE id=:id")
    void setTaskStatus(long id, CloudUploadStatus status, long updatedAt, String lastError);

    @Query("UPDATE cloud_upload_tasks SET uploadedBytes=:uploadedBytes, updatedAt=:updatedAt WHERE id=:id")
    void updateProgress(long id, long uploadedBytes, long updatedAt);

    @Query("DELETE FROM cloud_upload_tasks WHERE simulated=1")
    void clearSimulatedTasks();
}
