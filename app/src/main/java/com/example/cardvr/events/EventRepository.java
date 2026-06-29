package com.example.cardvr.events;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cardvr.database.AppDatabase;
import com.example.cardvr.database.EventDao;
import com.example.cardvr.database.EventEntity;
import com.example.cardvr.database.EventStatus;
import com.example.cardvr.database.EventType;
import com.example.cardvr.database.EventVideoSegmentCrossRef;

import java.util.List;

public final class EventRepository {
    private final EventDao dao;

    public EventRepository(Context context) {
        dao = AppDatabase.getInstance(context).eventDao();
    }

    public long insert(EventEntity event) { return dao.insert(event); }
    public void update(EventEntity event) { dao.update(event); }
    public LiveData<List<EventEntity>> observeAll() { return dao.observeAll(); }
    public List<EventEntity> getAll() { return dao.getAll(); }
    public EventEntity getById(long id) { return dao.getById(id); }
    public void setStatus(long id, EventStatus status) { dao.setStatus(id, status); }
    public void deleteById(long id) { dao.deleteById(id); }
    public void linkSegment(long eventId, long segmentId) {
        dao.insertCrossRef(new EventVideoSegmentCrossRef(eventId, segmentId));
    }
    public int countForTripByType(long tripId, EventType type) {
        return dao.countForTripByType(tripId, type);
    }
    public int countSegmentsForEvent(long eventId) {
        return dao.countSegmentsForEvent(eventId);
    }
}
