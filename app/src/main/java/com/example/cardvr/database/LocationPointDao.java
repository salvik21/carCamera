package com.example.cardvr.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationPointDao {
    @Insert long insert(LocationPointEntity point);
    @Query("SELECT * FROM location_points WHERE tripId=:tripId ORDER BY timestamp")
    List<LocationPointEntity> getForTrip(long tripId);
    @Query("SELECT * FROM location_points WHERE tripId=:tripId ORDER BY timestamp DESC LIMIT 1")
    LocationPointEntity getLast(long tripId);
}
