package com.example.cardvr.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VideoSegmentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(VideoSegmentEntity segment);

    @Update
    void update(VideoSegmentEntity segment);

    @Query("SELECT * FROM video_segments ORDER BY startTime DESC")
    LiveData<List<VideoSegmentEntity>> observeAll();

    @Query("SELECT * FROM video_segments ORDER BY startTime DESC")
    List<VideoSegmentEntity> getAll();

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM video_segments WHERE status = 'NORMAL'")
    long getNormalSizeBytes();

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM video_segments")
    long getTotalSizeBytes();

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM video_segments WHERE status = 'PROTECTED'")
    long getProtectedSizeBytes();

    @Query("SELECT * FROM video_segments WHERE status = 'NORMAL' AND uploadStatus NOT IN ('PENDING','IN_PROGRESS') AND transferStatus NOT IN ('PENDING','IN_PROGRESS') ORDER BY startTime ASC LIMIT 1")
    VideoSegmentEntity getOldestDeletableNormal();

    @Query("SELECT * FROM video_segments WHERE status = 'NORMAL' ORDER BY startTime DESC LIMIT 1")
    VideoSegmentEntity getLatestNormal();

    @Query("SELECT * FROM video_segments WHERE status = 'RECORDING'")
    List<VideoSegmentEntity> getRecordingSegments();

    @Query("UPDATE video_segments SET status = :status, protectedReason = :reason WHERE id = :id")
    void setStatus(long id, SegmentStatus status, String reason);

    @Query("DELETE FROM video_segments WHERE id = :id")
    void deleteById(long id);

    @Query("UPDATE video_segments SET tripId=:tripId, startLatitude=:latitude, startLongitude=:longitude WHERE id=:id")
    void attachToTrip(long id, Long tripId, Double latitude, Double longitude);

    @Query("UPDATE video_segments SET endLatitude=:latitude, endLongitude=:longitude, maxSpeedKmh=:maxSpeed WHERE id=:id")
    void finishMetadata(long id, Double latitude, Double longitude, double maxSpeed);

    @Query("SELECT COUNT(*) FROM video_segments WHERE tripId=:tripId")
    int countForTrip(long tripId);

    @Query("SELECT COUNT(*) FROM video_segments WHERE tripId=:tripId AND status='PROTECTED'")
    int countProtectedForTrip(long tripId);

    @Query("SELECT COALESCE(SUM(sizeBytes),0) FROM video_segments WHERE tripId=:tripId")
    long sizeForTrip(long tripId);

    @Query("SELECT * FROM video_segments WHERE startTime <= :untilTime AND (endTime IS NULL OR endTime >= :fromTime)")
    List<VideoSegmentEntity> getSegmentsOverlapping(long fromTime, long untilTime);

    @Query("UPDATE video_segments SET cloudStatus=:status, remoteFileId=:remoteFileId, cloudChecksum=:checksum, cloudUploadedAt=:uploadedAt WHERE id=:id")
    void updateCloudResult(long id, CloudStatus status, String remoteFileId, String checksum, Long uploadedAt);

    @Query("UPDATE video_segments SET cloudStatus=:status WHERE id=:id")
    void setCloudStatus(long id, CloudStatus status);
}
