package com.example.cardvr.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ErrorLogDao {
    @Insert
    long insert(ErrorLogEntity entity);

    @Query("SELECT * FROM error_logs ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<ErrorLogEntity>> observeRecent(int limit);

    @Query("SELECT * FROM error_logs ORDER BY timestamp DESC LIMIT :limit")
    List<ErrorLogEntity> getRecent(int limit);

    @Query("DELETE FROM error_logs WHERE id NOT IN (SELECT id FROM error_logs ORDER BY timestamp DESC LIMIT :keep)")
    void trim(int keep);
}
