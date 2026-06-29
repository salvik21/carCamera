package com.example.cardvr.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EventDao {
    @Insert
    long insert(EventEntity event);

    @Update
    void update(EventEntity event);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertCrossRef(EventVideoSegmentCrossRef crossRef);

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    LiveData<List<EventEntity>> observeAll();

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    List<EventEntity> getAll();

    @Query("SELECT * FROM events WHERE id=:id")
    EventEntity getById(long id);

    @Query("UPDATE events SET status=:status WHERE id=:id")
    void setStatus(long id, EventStatus status);

    @Query("DELETE FROM events WHERE id=:id")
    void deleteById(long id);

    @Query("SELECT COUNT(*) FROM events WHERE tripId=:tripId AND type=:type")
    int countForTripByType(long tripId, EventType type);

    @Query("SELECT COUNT(*) FROM event_video_segments WHERE eventId=:eventId")
    int countSegmentsForEvent(long eventId);
}
