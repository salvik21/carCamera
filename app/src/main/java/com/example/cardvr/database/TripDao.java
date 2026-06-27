package com.example.cardvr.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TripDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) long insert(TripEntity trip);
    @Update void update(TripEntity trip);
    @Query("SELECT * FROM trips WHERE status='ACTIVE' ORDER BY startTime DESC LIMIT 1")
    TripEntity getActive();
    @Query("SELECT * FROM trips WHERE id=:id LIMIT 1") TripEntity getById(long id);
    @Query("SELECT * FROM trips ORDER BY startTime DESC") LiveData<List<TripEntity>> observeAll();
    @Query("SELECT * FROM trips WHERE status='COMPLETED' ORDER BY endTime DESC LIMIT 1")
    TripEntity getLatestCompleted();
    @Query("UPDATE trips SET status=:status, endTime=:endTime WHERE id=:id")
    void finish(long id, TripStatus status, long endTime);
}
