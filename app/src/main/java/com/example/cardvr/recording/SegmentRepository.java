package com.example.cardvr.recording;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cardvr.database.AppDatabase;
import com.example.cardvr.database.SegmentStatus;
import com.example.cardvr.database.VideoSegmentDao;
import com.example.cardvr.database.VideoSegmentEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SegmentRepository {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private final VideoSegmentDao dao;

    public SegmentRepository(Context context) {
        dao = AppDatabase.getInstance(context).videoSegmentDao();
    }

    public static ExecutorService ioExecutor() {
        return IO;
    }

    public long insert(VideoSegmentEntity entity) { return dao.insert(entity); }
    public void update(VideoSegmentEntity entity) { dao.update(entity); }
    public LiveData<List<VideoSegmentEntity>> observeAll() { return dao.observeAll(); }
    public List<VideoSegmentEntity> getAll() { return dao.getAll(); }
    public long getNormalSizeBytes() { return dao.getNormalSizeBytes(); }
    public long getTotalSizeBytes() { return dao.getTotalSizeBytes(); }
    public long getProtectedSizeBytes() { return dao.getProtectedSizeBytes(); }
    public VideoSegmentEntity getOldestDeletableNormal() { return dao.getOldestDeletableNormal(); }
    public VideoSegmentEntity getLatestNormal() { return dao.getLatestNormal(); }
    public List<VideoSegmentEntity> getRecordingSegments() { return dao.getRecordingSegments(); }
    public void setStatus(long id, SegmentStatus status, String reason) {
        dao.setStatus(id, status, reason);
    }
    public void deleteById(long id) { dao.deleteById(id); }
    public void attachToTrip(long id, Long tripId, Double latitude, Double longitude) {
        dao.attachToTrip(id, tripId, latitude, longitude);
    }
    public void finishMetadata(long id, Double latitude, Double longitude, double maxSpeed) {
        dao.finishMetadata(id, latitude, longitude, maxSpeed);
    }
    public int countForTrip(long id) { return dao.countForTrip(id); }
    public int countProtectedForTrip(long id) { return dao.countProtectedForTrip(id); }
    public long sizeForTrip(long id) { return dao.sizeForTrip(id); }
}
